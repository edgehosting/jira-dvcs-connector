package com.atlassian.jira.plugins.bitbucket.spi;



import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugins.bitbucket.DefaultSynchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultSynchronisationOperation implements SynchronisationOperation
{
    private static final Logger log = LoggerFactory.getLogger(DefaultSynchronizer.class);

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
        if (key.getChangesets() == null)
        {
            // we are doing full sync, lets delete all existing changesets
            repositoryManager.removeAllChangesets(key.getRepository().getId());
        }

        Iterable<Changeset> changesets = getChangsetsIterator();
        Date lastCommitDate = null;
        if (key.getChangesets() == null)
        {
            // we are doing full synchronisation (maybe we should delete all
            // issueMappings)
            repositoryManager.setLastCommitDate(key.getRepository(), null);
        } else
        {
            lastCommitDate = repositoryManager.getLastCommitDate(key.getRepository());
        }

        int changesetCount = 0;
        int jiraCount = 0;
        int synchroErrorCount = 0;

        for (Changeset changeset : changesets)
        {
            if (lastCommitDate == null || lastCommitDate.before(changeset.getTimestamp()))
            {
                lastCommitDate = changeset.getTimestamp();
                repositoryManager.setLastCommitDate(key.getRepository(), lastCommitDate);
            }
            changesetCount++;
            String message = changeset.getMessage();
            log.debug("syncing changeset [{}] [{}]", changeset.getNode(), changeset.getMessage());
            if (message.contains(key.getRepository().getProjectKey()))
            {
                Set<String> extractedIssues = extractProjectKey(key.getRepository().getProjectKey(), message);
                // get detail changeset because in this response is not information about files
                Changeset detailChangeset = null;
                if (CollectionUtils.isNotEmpty(extractedIssues))
                {
                    try
                    {
                        detailChangeset = repositoryManager.getChangeset(key.getRepository(), changeset.getNode());
                    } catch (SourceControlException e)
                    {
                        log.warn("Unable to retrieve details for changeset " + changeset.getNode(), e);
                        synchroErrorCount++;
                    }
                }
                for (String extractedIssue : extractedIssues)
                {
                    jiraCount++;
                    String issueId = extractedIssue.toUpperCase();
                    try
                    {
                        repositoryManager.addChangeset(key.getRepository(), issueId, detailChangeset == null ? changeset : detailChangeset);
                    } catch (SourceControlException e)
                    {
                        log.error("Error adding changeset " + changeset, e);
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

    public Iterable<Changeset> getChangsetsIterator()
    {
        log.debug("synchronize [ {} ] with [ {} ]", key.getRepository().getProjectKey(),
                key.getRepository().getRepositoryUri().getRepositoryUrl());

        
        Iterable<Changeset> changesets = key.getChangesets() == null ? communicator.getChangesets(repositoryManager, key.getRepository()) : key.getChangesets();
        return changesets;
    }

}
