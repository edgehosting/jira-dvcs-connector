package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.google.common.eventbus.Subscribe;

/**
 * Listens to changeset events.
 */
public class ChangesetTestListener extends TestListener<ChangesetMapping>
{
    @Subscribe
    public void onCreate(ChangesetMapping changesetMapping)
    {
        created.add(changesetMapping);
    }
}
