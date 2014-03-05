package com.atlassian.jira.plugins.dvcs.event;

import com.google.common.eventbus.Subscribe;

/**
 * Listens to all events.
 */
public class AnythingTestListener extends TestListener<Object>
{
    @Subscribe
    public void onCreate(Object anything)
    {
        created.add(anything);
    }
}
