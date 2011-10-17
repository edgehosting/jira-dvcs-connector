package com.atlassian.jira.plugins.bitbucket.spi;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.OperationResult;
import com.atlassian.jira.plugins.bitbucket.api.Progress;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.google.common.base.Function;

public abstract class AbstractSynchronisationOperation implements SynchronisationOperation
{
	protected final SynchronizationKey key;
    protected final RepositoryManager repositoryManager;
	private final Function<SynchronizationKey, Progress> progressProvider;
    
    public AbstractSynchronisationOperation(SynchronizationKey key, RepositoryManager repositoryManager,
			Function<SynchronizationKey, Progress> progressProvider)
	{
    	this.key = key;
		this.repositoryManager = repositoryManager;
		this.progressProvider = progressProvider;
	}

	public OperationResult synchronise() throws Exception
    {
        Iterable<Changeset> changesets = getChangsetsIterator();

        int jiraCount = 0;

        for (Changeset changeset : changesets)
        {
            String message = changeset.getMessage();
            if (message.contains(key.getRepository().getProjectKey()))
            {
                Set<String> extractedIssues = extractProjectKey(key.getRepository().getProjectKey(), message);
                for (String extractedIssue : extractedIssues)
                {
                    jiraCount ++;
                    String issueId = extractedIssue.toUpperCase();
                    repositoryManager.addChangeset(issueId, changeset);
                    progressProvider.apply(key).inProgress(changeset.getNode(), jiraCount);
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
    
    public abstract Iterable<Changeset> getChangsetsIterator();
}
