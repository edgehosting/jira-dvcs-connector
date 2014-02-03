package com.atlassian.jira.plugins.dvcs.base;

/**
 * @see #register(TestListener)
 * @author Stanislav Dvorscak
 */
public interface TestListenerDelegate
{

    /**
     * Registers provided {@link TestListener}.
     * 
     * @param listener
     *            for registration
     */
    void register(TestListener listener);

}
