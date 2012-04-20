package com.atlassian.jira.plugins.bitbucket.api.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Communicator;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;

public class DefaultSynchronisationOperation implements SynchronisationOperation
{
    private static final Logger log = LoggerFactory.getLogger(DefaultSynchronisationOperation.class);

    protected final SynchronizationKey key;
    protected final RepositoryManager repositoryManager;
    private final ProgressWriter progressProvider;
    private final Communicator communicator;
    private final IssueManager issueManager;

    public DefaultSynchronisationOperation(SynchronizationKey key, RepositoryManager repositoryManager,
                                           Communicator communicator, ProgressWriter progressProvider,
                                           IssueManager issueManager)
    {
        this.key = key;
        this.repositoryManager = repositoryManager;
        this.communicator = communicator;
        this.progressProvider = progressProvider;
        this.issueManager = issueManager;
    }

    @Override
    public void synchronise()
    {

        final SourceControlRepository repository = key.getRepository();

        Date lastCommitDate = null;
        if (key.isSoftSync())
        {
            lastCommitDate = repositoryManager.getLastCommitDate(repository);
        } else
        {
            // we are doing full sync, lets delete all existing changesets
            repositoryManager.removeAllChangesets(repository.getId());
            repositoryManager.setLastCommitDate(repository, null);
        }

        int changesetCount = 0;
        int jiraCount = 0;
        int synchroErrorCount = 0;

        for (Changeset changeset : communicator.getChangesets(repository, lastCommitDate))
        {
            if (lastCommitDate == null || lastCommitDate.before(changeset.getTimestamp()))
            {
                lastCommitDate = changeset.getTimestamp();
                repositoryManager.setLastCommitDate(repository, lastCommitDate);
            }
            changesetCount++;
            String message = changeset.getMessage();
            log.debug("syncing changeset [{}] [{}]", changeset.getNode(), changeset.getMessage());
            if (message.contains(repository.getProjectKey()))
            {
                Set<String> extractedIssues = extractProjectKey(repository.getProjectKey(), message);
                // get detial changeset because in this response is not information about files
                Changeset detailChangeset = null;
                if (CollectionUtils.isNotEmpty(extractedIssues))
                {
                    try
                    {
                        detailChangeset = repositoryManager.getChangeset(repository, changeset);
                    } catch (Exception e)
                    {
                        log.warn("Unable to retrieve details for changeset " + changeset.getNode(), e);
                        synchroErrorCount++;
                    }
                    for (String extractedIssue : extractedIssues)
                    {
                        jiraCount++;
                        String issueId = extractedIssue.toUpperCase();
                        try
                        {
                            repositoryManager.addChangeset(repository, issueId, detailChangeset == null ? changeset : detailChangeset);
                        } catch (SourceControlException e)
                        {
                            log.error("Error adding changeset " + changeset, e);
                        }
                    }
                }
            }
            progressProvider.inProgress(changesetCount, jiraCount, synchroErrorCount);
        }
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
