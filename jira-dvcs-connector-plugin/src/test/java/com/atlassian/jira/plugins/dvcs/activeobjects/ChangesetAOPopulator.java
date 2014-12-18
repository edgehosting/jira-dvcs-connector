package com.atlassian.jira.plugins.dvcs.activeobjects;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToChangesetMapping;
import com.google.common.collect.ImmutableMap;
import net.java.ao.EntityManager;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChangesetAOPopulator extends AOPopulator
{
    public ChangesetAOPopulator(final EntityManager entityManager)
    {
        super(entityManager);
    }

    public ChangesetMapping createCSM()
    {
        return create(ChangesetMapping.class, new HashMap<String, Object>());
    }

    public ChangesetMapping createCSM(String node, String issueKey, RepositoryMapping repositoryMapping)
    {
        final ImmutableMap<String, Object> csParams = ImmutableMap.<String, Object>of(ChangesetMapping.NODE, node);
        return createCSM(csParams, issueKey, repositoryMapping);
    }

    public ChangesetMapping createCSM(Map<String, Object> params)
    {
        return create(ChangesetMapping.class, params);
    }

    public ChangesetMapping createCSM(Map<String, Object> params, String issueKey, RepositoryMapping repositoryMapping)
    {
        ChangesetMapping csm = create(ChangesetMapping.class, params);

        associateToIssue(csm, issueKey);
        associateToRepository(csm, repositoryMapping);

        return csm;
    }

    public Map<String, Object> getDefaultCSParams()
    {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

        builder.put(ChangesetMapping.NODE, "9ea7b66fc2be4f6c715a9b28f27618465b7ed246")
                .put(ChangesetMapping.FILE_DETAILS_JSON, "[]")
                .put(ChangesetMapping.RAW_AUTHOR, "raw atlas")
                .put(ChangesetMapping.AUTHOR, "atlas")
                .put(ChangesetMapping.DATE, new Date())
                .put(ChangesetMapping.RAW_NODE, "9ea7b66fc2be4f6c715a9b28f27618465b7ed246")
                .put(ChangesetMapping.BRANCH, "master")
                .put(ChangesetMapping.MESSAGE, "TST-1")
                .put(ChangesetMapping.PARENTS_DATA, "[]")
                .put(ChangesetMapping.FILE_COUNT, 1)
                .put(ChangesetMapping.AUTHOR_EMAIL, "admin@example.com")
                .put(ChangesetMapping.VERSION, 3)
                .put(ChangesetMapping.SMARTCOMMIT_AVAILABLE, true);

        return builder.build();
    }

    public IssueToChangesetMapping associateToIssue(ChangesetMapping changesetMapping, String issueKey)
    {
        IssueToChangesetMapping issueToChangesetMapping = create(IssueToChangesetMapping.class, new HashMap<String, Object>());
        issueToChangesetMapping.setIssueKey(issueKey);
        issueToChangesetMapping.setChangeset(changesetMapping);
        issueToChangesetMapping.save();

        return issueToChangesetMapping;
    }

    public RepositoryToChangesetMapping associateToRepository(ChangesetMapping changesetMapping, RepositoryMapping repository)
    {
        Map<String, Object> rtcMapping = ImmutableMap.of(
                RepositoryToChangesetMapping.REPOSITORY_ID, (Object) repository.getID(),
                RepositoryToChangesetMapping.CHANGESET_ID, changesetMapping.getID());
        return create(RepositoryToChangesetMapping.class, rtcMapping);
    }
}
