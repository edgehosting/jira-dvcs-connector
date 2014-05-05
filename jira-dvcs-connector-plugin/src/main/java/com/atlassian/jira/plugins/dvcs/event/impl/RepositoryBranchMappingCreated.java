package com.atlassian.jira.plugins.dvcs.event.impl;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.BranchMapping;

import java.util.Set;
import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Internal event indicating that a {@link com.atlassian.jira.plugins.dvcs.activeobjects.v3.BranchMapping} was created.
 */
public class RepositoryBranchMappingCreated
{
    @Nonnull
    private final BranchMapping branchMapping;

    @Nonnull
    private final Set<String> issueKeys;

    public RepositoryBranchMappingCreated(final BranchMapping branchMapping, final Set<String> issueKeys)
    {
        this.issueKeys = issueKeys;
        this.branchMapping = checkNotNull(branchMapping, "branchMapping");
    }

    @Nonnull
    public BranchMapping getBranchMapping()
    {
        return branchMapping;
    }

    @Nonnull
    public Set<String> getIssueKeys()
    {
        return issueKeys;
    }
}
