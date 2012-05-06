package com.atlassian.jira.plugins.dvcs.sync.impl;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.sync.ProgressWriter;
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultSynchronisationOperation implements SynchronisationOperation
{
    private static final Logger log = LoggerFactory.getLogger(DefaultSynchronisationOperation.class);

    protected final SynchronizationKey key;
    protected final RepositoryService repositoryService;
    private final ProgressWriter progressProvider;
    private final DvcsCommunicator communicator;
    private final IssueManager issueManager;

    public DefaultSynchronisationOperation(SynchronizationKey key, RepositoryService repositoryService,
                                           DvcsCommunicator communicator, ProgressWriter progressProvider,
                                           IssueManager issueManager)
    {
        this.key = key;
        this.repositoryService = repositoryService;
        this.communicator = communicator;
        this.progressProvider = progressProvider;
        this.issueManager = issueManager;
    }

    @Override
    public void synchronise()
    {

        final Repository repository = key.getRepository();
        // TODO: odkomentovat a refaktorovat na repoService
//        Date lastCommitDate = null;
//        if (key.isSoftSync())
//        {
//            lastCommitDate = repositoryService.getLastCommitDate(repository);
//        } else
//        {
//            // we are doing full sync, lets delete all existing changesets
//            repositoryService.removeAllChangesets(repository.getId());
//            repositoryService.setLastCommitDate(repository, null);
//        }
//
//        int changesetCount = 0;
//        int jiraCount = 0;
//        int synchroErrorCount = 0;
//
//        for (Changeset changeset : communicator.getChangesets(repository, lastCommitDate))
//        {
//            if (lastCommitDate == null || lastCommitDate.before(changeset.getDate()))
//            {
//                lastCommitDate = changeset.getDate();
//                repositoryService.setLastCommitDate(repository, lastCommitDate);
//            }
//            changesetCount++;
//            String message = changeset.getMessage();
//            log.debug("syncing changeset [{}] [{}]", changeset.getNode(), changeset.getMessage());
//            if (message.contains(repository.getProjectKey()))
//            {
//                Set<String> extractedIssues = extractProjectKey(repository.getProjectKey(), message);
//                // get detial changeset because in this response is not information about files
//                Changeset detailChangeset = null;
//                if (CollectionUtils.isNotEmpty(extractedIssues))
//                {
//                    try
//                    {
//                        detailChangeset = repositoryService.getChangeset(repository, changeset);
//                    } catch (Exception e)
//                    {
//                        log.warn("Unable to retrieve details for changeset " + changeset.getNode(), e);
//                        synchroErrorCount++;
//                    }
//                    for (String extractedIssue : extractedIssues)
//                    {
//                        jiraCount++;
//                        String issueId = extractedIssue.toUpperCase();
//                        try
//                        {
//                            repositoryService.addChangeset(repository, issueId, detailChangeset == null ? changeset : detailChangeset);
//                        } catch (SourceControlException e)
//                        {
//                            log.error("Error adding changeset " + changeset, e);
//                        }
//                    }
//                }
//            }
//            progressProvider.inProgress(changesetCount, jiraCount, synchroErrorCount);
//        }
    }

    private Set<String> extractProjectKey(String projectKey, String message)
    {
        // should check that issue exists?
        Pattern projectKeyPattern = Pattern.compile("(" + projectKey + "-\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher match = projectKeyPattern.matcher(message);

        Set<String> matches = new HashSet<String>();

        while (match.find())
        {
            // Get all groups for this match
            for (int i = 0; i <= match.groupCount(); i++)
            {
                String issueKey = match.group(i);
                if (issueManager.getIssueObject(issueKey) != null)
                    matches.add(issueKey);
            }
        }
        return matches;
    }

}
