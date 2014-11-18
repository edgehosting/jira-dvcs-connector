package com.atlassian.jira.plugins.dvcs.activeobjects;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activity.PullRequestParticipantMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestIssueKeyMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.google.common.collect.ImmutableMap;
import net.java.ao.EntityManager;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PullRequestAOPopulator extends AOPopulator
{
    public PullRequestAOPopulator(final EntityManager entityManager)
    {
        super(entityManager);
    }

    public RepositoryPullRequestMapping createPR(String name, String issueKey, RepositoryMapping repositoryMapping)
    {
        RepositoryPullRequestMapping prMapping = create(RepositoryPullRequestMapping.class,
                getDefaultPRParams(name, repositoryMapping.getID()));
        prMapping.setToRepositoryId(repositoryMapping.getID());
        prMapping.save();

        associateToIssue(prMapping, issueKey);

        return prMapping;
    }

    public Map<String, Object> getDefaultPRParams(String name, int repositoryId)
    {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

        builder.put(RepositoryPullRequestMapping.REMOTE_ID, 50L)
                .put(RepositoryPullRequestMapping.EXECUTED_BY, "bob")
                .put(RepositoryPullRequestMapping.NAME, name)
                .put(RepositoryPullRequestMapping.URL, "http://atlassian.com")
                .put(RepositoryPullRequestMapping.LAST_STATUS, PullRequestStatus.OPEN.name())
                .put(RepositoryPullRequestMapping.CREATED_ON, new Date())
                .put(RepositoryPullRequestMapping.UPDATED_ON, new Date())
                .put(RepositoryPullRequestMapping.AUTHOR, "atlas")
                .put(RepositoryPullRequestMapping.COMMENT_COUNT, 3)
                .put(RepositoryPullRequestMapping.SOURCE_BRANCH, "ABC-123")
                .put(RepositoryPullRequestMapping.SOURCE_REPO, "fusion")
                .put(RepositoryPullRequestMapping.DESTINATION_BRANCH, "master")
                .put(RepositoryPullRequestMapping.DOMAIN, repositoryId);

        return builder.build();
    }

    public PullRequestParticipantMapping createParticipant(String username, Boolean approved, String role,
            RepositoryPullRequestMapping prMapping)
    {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(PullRequestParticipantMapping.USERNAME, username);
        params.put(PullRequestParticipantMapping.APPROVED, approved);
        params.put(PullRequestParticipantMapping.ROLE, role);
        params.put(PullRequestParticipantMapping.PULL_REQUEST_ID, prMapping.getID());
        params.put(PullRequestParticipantMapping.DOMAIN, prMapping.getDomainId());

        return create(PullRequestParticipantMapping.class, params);
    }


    public RepositoryPullRequestIssueKeyMapping associateToIssue(RepositoryPullRequestMapping prMapping, String issueKey)
    {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(RepositoryPullRequestIssueKeyMapping.ISSUE_KEY, issueKey);
        params.put(RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID, prMapping.getID());
        params.put(RepositoryPullRequestIssueKeyMapping.DOMAIN, prMapping.getDomainId());

        return create(RepositoryPullRequestIssueKeyMapping.class, params);
    }
}
