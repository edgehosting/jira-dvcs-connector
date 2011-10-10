package com.atlassian.jira.plugins.bitbucket.mapper.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.mapper.OperationResult;
import com.atlassian.jira.plugins.bitbucket.mapper.Progress;
import com.atlassian.jira.plugins.bitbucket.mapper.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketCommunicator;
import com.google.common.base.Function;

public class BitbucketSynchronisation implements Callable<OperationResult>
{
    private final Logger logger = LoggerFactory.getLogger(DefaultSynchronizer.class);

    private final SynchronizationKey key;
	private RepositoryManager repositoryManager;
	private BitbucketCommunicator bitbucketCommunicator;
	private final Function<SynchronizationKey, Progress> progressProvider;

    public BitbucketSynchronisation(SynchronizationKey key, RepositoryManager repositoryManager,
			BitbucketCommunicator bitbucketCommunicator, Function<SynchronizationKey, Progress> progressProvider)
	{
		this.key = key;
		this.repositoryManager = repositoryManager;
		this.bitbucketCommunicator = bitbucketCommunicator;
		this.progressProvider = progressProvider;
	}

    public OperationResult call() throws Exception
    {
        logger.debug("synchronize [ {} ] with [ {} ]", key.getProjectKey(), key.getRepositoryUri());

        SourceControlRepository repository = repositoryManager.getRepository(key.getProjectKey(), key.getRepositoryUri().getRepositoryUrl());
        Iterable<Changeset> changesets = key.getChangesets() == null ? bitbucketCommunicator.getChangesets(repository) : key.getChangesets();

        int jiraCount = 0;

        for (Changeset changeset : changesets)
        {
            String message = changeset.getMessage();

            if (message.contains(key.getProjectKey()))
            {
                Set<String> extractedIssues = extractProjectKey(key.getProjectKey(), message);
                for (String extractedIssue : extractedIssues)
                {
                    jiraCount ++;
                    String issueId = extractedIssue.toUpperCase();
                    repositoryManager.addChangeset(issueId, changeset);
                    progressProvider.apply(key).inProgress(changeset.getRevision(), jiraCount);
                }
            }
        }

        return OperationResult.YES;
    }
    
    private static Set<String> extractProjectKey(String projectKey, String message)
    {
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

}
