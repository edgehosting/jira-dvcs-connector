package com.atlassian.jira.plugins.dvcs.event.impl;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.PullRequestTransformer;
import com.atlassian.jira.plugins.dvcs.event.PullRequestCreatedEvent;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.sync.SyncEventListener;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Relays internal PR events on to the JIRA EventPublisher.
 */
@Component
public class PullRequestEventRelay implements SyncEventListener
{
    private final EventPublisher eventPublisher;
    private final PullRequestTransformer pullRequestTransformer;

    @Autowired
    public PullRequestEventRelay(EventPublisher eventPublisher, RepositoryService repositoryService)
    {
        this.eventPublisher = eventPublisher;
        this.pullRequestTransformer = new PullRequestTransformer(repositoryService);
    }

    /**
     * Listens to RepositoryPullRequestMappingCreated events and publishes them on the JIRA EventPublisher as
     * PullRequestCreatedEvent.
     */
    @Subscribe
    public void onPullRequestCreated(RepositoryPullRequestMappingCreated pullRequestMappingCreated)
    {
        PullRequest createdPR = pullRequestTransformer.transform(pullRequestMappingCreated.getPullRequestMapping());
        eventPublisher.publish(new PullRequestCreatedEvent(createdPR));
    }
}
