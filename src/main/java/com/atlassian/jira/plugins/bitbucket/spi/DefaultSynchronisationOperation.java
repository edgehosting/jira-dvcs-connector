package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.DefaultSynchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    public DefaultSynchronisationOperation(SynchronizationKey key, RepositoryManager repositoryManager,
                                           Communicator communicator, ProgressWriter progressProvider)
	{
    	this.key = key;
		this.repositoryManager = repositoryManager;
        this.communicator = communicator;
        this.progressProvider = progressProvider;
    }

	public void synchronise()
    {
        Iterable<Changeset> changesets = getChangsetsIterator();

        int changesetCount = 0;
        int jiraCount = 0;

        for (Changeset changeset : changesets)
        {
        	changesetCount ++;
            String message = changeset.getMessage();
            log.debug("syncing changeset [{}] [{}]", changeset.getNode(), changeset.getMessage());
            if (message.contains(key.getRepository().getProjectKey()))
            {
                Set<String> extractedIssues = extractProjectKey(key.getRepository().getProjectKey(), message);
                for (String extractedIssue : extractedIssues)
                {
                    jiraCount ++;
                    String issueId = extractedIssue.toUpperCase();
                    repositoryManager.addChangeset(key.getRepository(), issueId, changeset);
                }
            }
            progressProvider.inProgress(changesetCount, jiraCount);
        }
    }

    private static Set<String> extractProjectKey(String projectKey, String message)
    {
        // TODO check that issue exists
        Pattern projectKeyPattern = Pattern.compile("(" + projectKey + "-\\d*)", Pattern.CASE_INSENSITIVE);
        Matcher match = projectKeyPattern.matcher(message);

        Set<String> matches = new HashSet<String>();

        while (match.find())
        {
            // Get all groups for this match
            for (int i = 0; i <= match.groupCount(); i++)
                matches.add(match.group(i));
        }

        return matches;
    }
    
    public Iterable<Changeset> getChangsetsIterator()
    {
        log.debug("synchronize [ {} ] with [ {} ]", key.getRepository().getProjectKey(),
                key.getRepository().getRepositoryUri().getRepositoryUrl());

        Iterable<Changeset> changesets = key.getChangesets() == null ? communicator.getChangesets(key.getRepository()) : key.getChangesets();
        return changesets;
    }

}
