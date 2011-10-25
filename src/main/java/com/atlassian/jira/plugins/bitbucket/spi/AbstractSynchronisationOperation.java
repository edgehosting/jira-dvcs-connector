package com.atlassian.jira.plugins.bitbucket.spi;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;

public abstract class AbstractSynchronisationOperation implements SynchronisationOperation
{
	protected final SynchronizationKey key;
    protected final RepositoryManager repositoryManager;
	private final ProgressWriter progressProvider;
    
    public AbstractSynchronisationOperation(SynchronizationKey key, RepositoryManager repositoryManager,
			ProgressWriter progressProvider)
	{
    	this.key = key;
		this.repositoryManager = repositoryManager;
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
