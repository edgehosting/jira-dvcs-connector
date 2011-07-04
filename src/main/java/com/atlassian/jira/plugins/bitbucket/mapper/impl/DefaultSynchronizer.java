package com.atlassian.jira.plugins.bitbucket.mapper.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.bitbucket.*;
import com.atlassian.jira.plugins.bitbucket.mapper.BitbucketMapper;
import com.atlassian.jira.plugins.bitbucket.mapper.SynchronizationProgress;
import com.atlassian.jira.plugins.bitbucket.mapper.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.mapper.activeobjects.SyncProgress;
import com.atlassian.jira.vcs.RepositoryManager;
import com.sun.org.apache.bcel.internal.util.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Synchronization services
 */
public class DefaultSynchronizer implements Synchronizer
{
    private final Logger logger = LoggerFactory.getLogger(DefaultSynchronizer.class);
    private final Bitbucket bitbucket;
    private final BitbucketMapper bitbucketMapper;
    private final ActiveObjects activeObjects;

    public DefaultSynchronizer(Bitbucket bitbucket, BitbucketMapper bitbucketMapper, ActiveObjects activeObjects)
    {
        this.bitbucket = bitbucket;
        this.bitbucketMapper = bitbucketMapper;
        this.activeObjects = activeObjects;
    }

    private Set<String> extractProjectKey(String projectKey, String message)
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

    public void synchronize(String projectKey, RepositoryUri repositoryUri)
    {
        logger.debug("synchronize [ {} ] with [ {} ]", projectKey, repositoryUri.getRepositoryUri());

        BitbucketAuthentication auth = bitbucketMapper.getAuthentication(projectKey, repositoryUri);
        List<BitbucketChangeset> changesets = bitbucket.getChangesets(auth, repositoryUri.getOwner(), repositoryUri.getSlug());
        for (BitbucketChangeset changeset : changesets)
        {
            String message = changeset.getMessage();

            if (message.contains(projectKey))
            {
                Set<String> extractedIssues = extractProjectKey(projectKey, message);
                for (String extractedIssue : extractedIssues)
                {
                    String issueId = extractedIssue.toUpperCase();
                    bitbucketMapper.addChangeset(issueId, changeset);
                }
            }
        }
    }

    public void synchronize(String projectKey, RepositoryUri repositoryUri, List<BitbucketChangeset> changesets)
    {
        for (BitbucketChangeset changeset : changesets)
        {
            String message = changeset.getMessage();

            if (message.contains(projectKey.toLowerCase()))
            {
                Set<String> extractedIssues = extractProjectKey(projectKey, message);
                for (String extractedIssue : extractedIssues)
                {
                    String issueId = extractedIssue.toUpperCase();
                    // TODO filter to repositoryUri
                    bitbucketMapper.addChangeset(issueId, changeset);
                }
            }
        }
    }

    public SynchronizationProgress getProgress(String projectKey, RepositoryUri repositoryUri)
    {
        //activeObjects.find(SyncProgress.class,"project_key = ? and owner = ? and slug = ?",projectKey, owner, slug);
        return null;
    }

}
