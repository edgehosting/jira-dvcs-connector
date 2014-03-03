package com.atlassian.jira.plugins.dvcs.base;

/**
 * Adapter/Abstract implementation for {@link TestListener}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public abstract class AbstractTestListener implements TestListener
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeClass()
    {
        // feel free to override
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeMethod()
    {
        // feel free to override
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterMethod()
    {
        // feel free to override
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterClass()
    {
        // feel free to override
    }

}
