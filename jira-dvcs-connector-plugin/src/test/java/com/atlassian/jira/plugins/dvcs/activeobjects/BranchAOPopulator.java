package com.atlassian.jira.plugins.dvcs.activeobjects;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.BranchMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToBranchMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.google.common.collect.ImmutableMap;
import net.java.ao.EntityManager;

import java.util.HashMap;
import java.util.Map;

public class BranchAOPopulator extends AOPopulator
{
    public BranchAOPopulator(final EntityManager entityManager)
    {
        super(entityManager);
    }

    public BranchMapping createBranch(String name, String issueKey, RepositoryMapping repositoryMapping)
    {
        BranchMapping branchMapping = create(BranchMapping.class, getDefaultPRParams(name, repositoryMapping.getID()));

        associateToIssue(branchMapping, issueKey);

        return branchMapping;
    }

    public Map<String, Object> getDefaultPRParams(String name, int repositoryId)
    {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

        builder.put(BranchMapping.NAME, name)
                .put(BranchMapping.REPOSITORY_ID, repositoryId);

        return builder.build();
    }

    public IssueToBranchMapping associateToIssue(BranchMapping branchMapping, String issueKey)
    {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(IssueToBranchMapping.ISSUE_KEY, issueKey);
        params.put(IssueToBranchMapping.BRANCH_ID, branchMapping.getID());

        return create(IssueToBranchMapping.class, params);
    }
}
