package com.atlassian.jira.plugins.dvcs.dao.ao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Maps;
import net.java.ao.RawEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.stereotype.Component;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Map.Entry;

import static java.beans.Introspector.getBeanInfo;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Generic factory for creating "beany" instances of ActiveObjects entities.
 */
@Component
public final class EntityBeanGenerator
{
    /**
     * Returns a new "beany" instance of the entity interface. By this I mean that the returned instances can be used as
     * a bean to set/get properties but all "active" methods like save() will throw UnsupportedOperationException.
     *
     * @param entityInterface an ActiveObjects entity interface
     * @return a new instance of {@code entityInterface}
     */
    @SuppressWarnings ("unchecked")
    public <T extends RawEntity> T createInstanceOf(Class<T> entityInterface)
    {
        if (!entityInterface.isInterface())
        {
            throw new IllegalArgumentException("Expected an interface, got: " + entityInterface);
        }

        return (T) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] { entityInterface },
                new EntityBeanInterceptor(entityInterface)
        );
    }

    /**
     * cglib interceptor for dynamically implementing an ActiveObjects bean.
     */
    private static class EntityBeanInterceptor implements InvocationHandler
    {
        /**
         * Default values for primitive types.
         */
        private static final ImmutableMap<Class<?>, Object> DEFAULTS = ImmutableMap.<Class<?>, Object>builder()
                .put(byte.class, (byte) 0)
                .put(short.class, (short) 0)
                .put(int.class, 0)
                .put(long.class, (long) 0)
                .put(float.class, 0.0f)
                .put(double.class, (double) 0)
                .put(char.class, '\u0000')
                .put(boolean.class, false)
                .build();

        /**
         * Interceptor for each bean method.
         */
        private final ImmutableMap<Method, InvocationHandler> beanMethodInterceptors;

        /**
         * Backing storage for the bean's properties.
         */
        private final Map<String, Object> beanProperties = Maps.newHashMap();

        public EntityBeanInterceptor(Class<?> entityInterface)
        {
            beanMethodInterceptors = getBeanMethods(entityInterface);
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
        {
            // for equals, hashCode, etc
            if (method.getDeclaringClass() == Object.class)
            {
                if ("equals".equals(method.getName()))
                {
                    return equals(proxy, args[0]);
                }

                if ("hashCode".equals(method.getName()))
                {
                    return hashCode();
                }

                if ("toString".equals(method.getName()))
                {
                    return beanToString();
                }

                return method.invoke(this, args);
            }

            InvocationHandler interceptor = beanMethodInterceptors.get(method);
            if (interceptor != null)
            {
                return interceptor.invoke(proxy, method, args);
            }

            // if it's a bean property
            throw new UnsupportedOperationException(method.getName() + " is not implemented (not a bean setter or getter)");
        }

        private ImmutableMap<Method, InvocationHandler> getBeanMethods(final Class<?> entityInterface)
        {
            try
            {
                Builder<Method, InvocationHandler> builder = ImmutableMap.builder();

                // first recurse up the hierarchy to find properties in all super interfaces
                for (Class<?> superInterface : entityInterface.getInterfaces())
                {
                    builder.putAll(getBeanMethods(superInterface));
                }

                // then add all of this interface's properties
                for (PropertyDescriptor descriptor : getBeanInfo(entityInterface).getPropertyDescriptors())
                {
                    Method readMethod = descriptor.getReadMethod();
                    if (readMethod != null)
                    {
                        builder.put(readMethod, new Getter(descriptor.getName()));
                    }

                    Method writeMethod = descriptor.getWriteMethod();
                    if (writeMethod != null)
                    {
                        builder.put(writeMethod, new Setter(descriptor.getName()));
                    }
                }

                return builder.build();
            }
            catch (IntrospectionException e)
            {
                throw new RuntimeException(e);
            }
        }

        private Object equals(final Object thisObj, final Object thatObj)
        {
            // use only reference equality for now
            return thisObj == thatObj;
        }

        private String beanToString()
        {
            ToStringBuilder toStringBuilder = new ToStringBuilder(this, SHORT_PREFIX_STYLE);
            for (Entry<String, Object> prop : beanProperties.entrySet())
            {
                toStringBuilder.append(prop.getKey(), prop.getValue());
            }

            return toStringBuilder.build();
        }

        /**
         * Property getter.
         */
        private class Getter extends PropertyInterceptor
        {
            public Getter(final String propertyName)
            {
                super(propertyName);
            }

            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
            {
                if (beanProperties.containsKey(propertyName))
                {
                    // the property has been set
                    return beanProperties.get(propertyName);
                }

                Class<?> returnType = method.getReturnType();
                if (DEFAULTS.containsKey(returnType))
                {
                    // the property has not been set. return the default value for a primitive
                    return DEFAULTS.get(returnType);
                }

                // everything else is null
                return null;
            }
        }

        /**
         * Property setter.
         */
        private class Setter extends PropertyInterceptor
        {
            public Setter(String propertyName)
            {
                super(propertyName);
            }

            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
            {
                beanProperties.put(propertyName, args[0]);
                return null;
            }
        }

        private abstract static class PropertyInterceptor implements InvocationHandler
        {
            protected final String propertyName;

            public PropertyInterceptor(final String propertyName)
            {
                this.propertyName = propertyName;
            }
        }
    }
}
