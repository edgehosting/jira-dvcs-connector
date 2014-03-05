package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.google.common.eventbus.Subscribe;

/**
 * Listens to pull request events.
 */
public class PullRequestTestListener extends TestListener<RepositoryPullRequestMapping>
{
    @Subscribe
    public void onCreate(RepositoryPullRequestMapping changesetMapping)
    {
        created.add(changesetMapping);
    }
}
