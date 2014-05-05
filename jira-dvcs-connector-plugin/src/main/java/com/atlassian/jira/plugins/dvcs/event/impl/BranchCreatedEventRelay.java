package com.atlassian.jira.plugins.dvcs.event.impl;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.BranchCreatedTransformer;
import com.atlassian.jira.plugins.dvcs.event.BranchCreatedEvent;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.sync.SyncEventListener;
import com.google.common.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Relays internal Branch created events on to the JIRA EventPublisher.
 */
@Component
public class BranchCreatedEventRelay implements SyncEventListener
{
    private final EventPublisher eventPublisher;
    private final BranchCreatedTransformer branchCreatedTransformer;

    @Autowired
    public BranchCreatedEventRelay(EventPublisher eventPublisher)
    {
        this.eventPublisher = eventPublisher;
        this.branchCreatedTransformer = new BranchCreatedTransformer();
    }

    /**
     * Listens to RepositoryBranchMappingCreated events and publishes them on the JIRA EventPublisher as
     * BranchCreatedEvent.
     */
    @Subscribe
    public void onBranchCreated(RepositoryBranchMappingCreated branchMappingCreated)
    {
        Branch branchCreated = branchCreatedTransformer.transform(branchMappingCreated.getBranchMapping());
        eventPublisher.publish(new BranchCreatedEvent(branchCreated, branchMappingCreated.getIssueKeys()));
    }
}
