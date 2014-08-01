package com.atlassian.jira.plugins.dvcs.base;

import java.util.LinkedList;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Represents base class for all tests
 * 
 * @author Stanislav Dvorscak
 * 
 */
public abstract class BaseDVCSTest implements TestListenerDelegate
{

    /**
     * Registered listeners.
     * 
     * @see #register(TestListener)
     */
    private List<TestListener> listeners = new LinkedList<TestListener>();

    /**
     * @see TestListener#beforeClass()
     * @see #register(TestListener)
     */
    @BeforeClass
    public void beforeClassListenerDelegate()
    {
        for (TestListener listener : listeners)
        {
            listener.beforeClass();
            ;
        }
    }

    /**
     * @see TestListener#afterMethod()
     * @see #register(TestListener)
     */
    @BeforeMethod
    public void beforeMethodListenerDelegate()
    {
        for (TestListener listener : listeners)
        {
            listener.beforeMethod();
        }
    }

    /**
     * @see TestListener#afterMethod()
     * @see #register(TestListener)
     */
    @AfterMethod(alwaysRun = true)
    public void afterMethodListenerDelegate()
    {
        for (TestListener listener : listeners)
        {
            listener.afterMethod();
        }
    }

    /**
     * @see TestListener#afterClass()
     * @see #register(TestListener)
     */
    @AfterClass(alwaysRun = true)
    public void afterClassListenerDelegate()
    {
        for (TestListener listener : listeners)
        {
            listener.afterClass();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(TestListener listener)
    {
        listeners.add(listener);
    }

}
