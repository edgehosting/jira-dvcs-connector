package com.atlassian.jira.plugins.dvcs.service;

import static com.google.common.collect.Lists.newArrayList;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Defaults;

import net.java.ao.schema.AccessorFieldNameResolver;
import net.java.ao.schema.Case;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.FieldNameResolver;
import net.java.ao.schema.GetterFieldNameResolver;
import net.java.ao.schema.IgnoredFieldNameResolver;
import net.java.ao.schema.IsAFieldNameResolver;
import net.java.ao.schema.MutatorFieldNameResolver;
import net.java.ao.schema.NullFieldNameResolver;
import net.java.ao.schema.PrimaryKeyFieldNameResolver;
import net.java.ao.schema.RelationalFieldNameResolver;
import net.java.ao.schema.SetterFieldNameResolver;
import net.java.ao.schema.UnderscoreFieldNameConverter;

/**
 * Implementation of the {@link ColumnNameResolverService}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class ColumnNameResolverServiceImpl implements ColumnNameResolverService
{

    /**
     * Holds mapping between the AO type & appropriate class-loader and helper proxy for column name resolving. The synchronization of the
     * map and inner map is done via direct object locking.
     */
    private final Map<ClassLoader, Map<Class<?>, Object>> descriptions = new ConcurrentHashMap<ClassLoader, Map<Class<?>, Object>>();

    /**
     * Injected {@link FieldNameConverter} dependency.
     */
    private FieldNameConverter fieldNameConverter;

    /**
     * Holds column name of the last called property.
     */
    private ThreadLocal<String> columnNameHolder = new ThreadLocal<String>();

    /**
     * Proxy invocation handler, which saves column name into the {@link #columnNameHolder} of the last called bean property.
     */
    private final InvocationHandler descriptionHandler = new InvocationHandler()
    {

        /**
         * Mapping between the AO method and appropriate column name. The synchronization is done via direct object locking.
         */
        private final Map<Method, String> methodToColumnName = new ConcurrentHashMap<Method, String>();

        /**
         * {@inheritDoc}
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            String columnName = methodToColumnName.get(method);
            if (columnName == null)
            {
                synchronized (methodToColumnName)
                {
                    columnName = methodToColumnName.get(method);
                    if (columnName == null)
                    {
                        methodToColumnName.put(method, columnName = resolveColumnName(method));
                    }
                }
            }

            columnNameHolder.set(columnName);

            // default return type value
            return Defaults.defaultValue(method.getReturnType());
        }

        /**
         * @param method
         *            appropriate column getter
         * @return resolved column name
         */
        private String resolveColumnName(Method method)
        {
            return fieldNameConverter.getName(method);
        }

    };

    /**
     * Overrides {@link FieldNameResolver#transform()} to the true.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private static final class TransformingFieldNameResolver implements InvocationHandler
    {
        /**
         * {@link FieldNameResolver#transform()}
         */
        private static final Method TRANSFORM_METHOD;

        /**
         * Static initialization.
         */
        static
        {
            try
            {
                TRANSFORM_METHOD = FieldNameResolver.class.getMethod("transform", new Class<?>[] {});
            } catch (NoSuchMethodException e)
            {
                throw new RuntimeException(e);

            } catch (SecurityException e)
            {
                throw new RuntimeException(e);
            }
        }

        /**
         * @see TransformingFieldNameResolver#TransformingFieldNameResolver(FieldNameResolver)
         */
        private final FieldNameResolver delegate;

        /**
         * Constructor.
         * 
         * @param delegate
         *            delegate of the original implementation
         */
        private TransformingFieldNameResolver(FieldNameResolver delegate)
        {
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            if (TRANSFORM_METHOD.equals(method))
            {
                return true;
            }

            return method.invoke(delegate, args);
        }

        /**
         * @param delegate
         * @return factory method
         */
        private static FieldNameResolver newInstance(FieldNameResolver delegate)
        {
            return (FieldNameResolver) Proxy.newProxyInstance(FieldNameConverter.class.getClassLoader(),
                    new Class<?>[] { FieldNameResolver.class }, new TransformingFieldNameResolver(delegate));
        }
    }

    /**
     * Constructor.
     */
    public ColumnNameResolverServiceImpl()
    {
        this.fieldNameConverter = new UnderscoreFieldNameConverter(Case.UPPER, newArrayList( //
                new IgnoredFieldNameResolver(), //
                new RelationalFieldNameResolver(), //
                TransformingFieldNameResolver.newInstance(new MutatorFieldNameResolver()), //
                TransformingFieldNameResolver.newInstance(new AccessorFieldNameResolver()), //
                TransformingFieldNameResolver.newInstance(new PrimaryKeyFieldNameResolver()), //
                new GetterFieldNameResolver(), //
                new SetterFieldNameResolver(), //
                new IsAFieldNameResolver(), //
                new NullFieldNameResolver() //
                ));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T desc(Class<T> clazz)
    {
        ClassLoader classLoader = clazz.getClassLoader();
        Map<Class<?>, Object> byClassLoader = descriptions.get(classLoader);
        if (byClassLoader == null)
        {
            synchronized (descriptions)
            {
                byClassLoader = descriptions.get(classLoader);
                if (byClassLoader == null)
                {
                    descriptions.put(classLoader, byClassLoader = new ConcurrentHashMap<Class<?>, Object>());
                }
            }
        }

        T result = (T) byClassLoader.get(clazz);
        if (result == null)
        {
            synchronized (byClassLoader)
            {
                result = (T) byClassLoader.get(clazz);
                if (result == null)
                {
                    result = (T) Proxy.newProxyInstance(classLoader, new Class<?>[] { clazz }, descriptionHandler);
                }
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> String column(Object desc)
    {
        String result = columnNameHolder.get();
        columnNameHolder.set(null);
        return result;
    }

}
