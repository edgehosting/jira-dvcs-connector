package com.atlassian.jira.plugins.dvcs.util;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.Fields;
import org.mockito.internal.util.reflection.InstanceField;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestNGListener;
import org.testng.ITestResult;
import org.testng.annotations.Listeners;

import static org.mockito.internal.util.reflection.Fields.annotatedBy;

/**
 * Mockito TestNG Listener, Acts as the {@link org.mockito.runners.MockitoJUnitRunner} for TestNG, taking into
 * consideration the different test object life cycle in TestNG. It forces re-initialization of all mocks before
 * each test method is executed.
 *
 * <p/>
 * Use this listener whenever using {@code Mockito} in {@code TestNG} tests, where different tests need to initialize
 * the mocks in different ways.
 *
 * <p/>
 * Inspired by {@code MockitoTestNgListener} in {@code Mockito} but works in a little bit more brute-force'y way.
 */
public class MockitoTestNgListener implements IInvokedMethodListener
{
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult)
    {
        if (isAnnotated(testResult.getTestClass().getRealClass()))
        {
            resetMocks(testResult.getInstance());
            MockitoAnnotations.initMocks(testResult.getInstance());
        }
    }

    public void afterInvocation(IInvokedMethod method, ITestResult testResult)
    {
    }

    private boolean isAnnotated(Class<?> realClass)
    {
        Listeners listeners = realClass.getAnnotation(Listeners.class);
        if (listeners == null)
        {
            return false;
        }
        for (Class<? extends ITestNGListener> listenerClass : listeners.value())
        {
            if (listenerClass.equals(MockitoTestNgListener.class))
            {
                return true;
            }
        }
        return false;
    }

    private void resetMocks(Object instance)
    {
        Iterable<InstanceField> toReset = Fields.allDeclaredFieldsOf(instance)
                .filter(annotatedBy(Mock.class, InjectMocks.class, Spy.class))
                .notNull()
                .instanceFields();
        for (InstanceField field : toReset)
        {
            field.set(null);
        }
    }
}
