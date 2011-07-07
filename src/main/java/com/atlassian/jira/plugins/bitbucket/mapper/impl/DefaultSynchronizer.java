package com.atlassian.jira.plugins.bitbucket.mapper.impl;

import com.atlassian.jira.plugins.bitbucket.bitbucket.Bitbucket;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketAuthentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangeset;
import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.mapper.*;
import com.atlassian.util.concurrent.Nullable;
import com.atlassian.util.concurrent.ThreadFactories;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Synchronization services
 */
public class DefaultSynchronizer implements Synchronizer
{
    private class Coordinator
    {
        private final ConcurrentMap<SynchronizationKey, Progress> operations =
                new MapMaker().makeComputingMap(new Function<SynchronizationKey, Progress>()
                {
                    public Progress apply(final SynchronizationKey from)
                    {
                        return new Progress(from, executorService.submit(new Callable<OperationResult>()
                        {

                            public OperationResult call() throws Exception
                            {
                                try
                                {
                                    return new Operation(from).call();
                                }
                                finally
                                {
                                    operations.remove(from);
                                }
                            }
                        }));
                    }
                });

        private final ExecutorService executorService;

        public Coordinator(ExecutorService executorService)
        {
            this.executorService = executorService;
        }
    }

    private class Operation implements Callable<OperationResult>
    {
        private final SynchronizationKey key;

        public Operation(SynchronizationKey key)
        {
            this.key = key;
        }

        public OperationResult call() throws Exception
        {
            logger.debug("synchronize [ {} ] with [ {} ]", key.getProjectKey(), key.getRepositoryUri());

            BitbucketAuthentication auth = bitbucketMapper.getAuthentication(key.getProjectKey(), key.getRepositoryUri());
            Iterable<BitbucketChangeset> changesets = key.getChangesets() == null ?
                    bitbucket.getChangesets(auth, key.getRepositoryUri().getOwner(), key.getRepositoryUri().getSlug()) :
                    key.getChangesets();

            for (BitbucketChangeset changeset : changesets)
            {
                String message = changeset.getMessage();

                if (message.contains(key.getProjectKey()))
                {
                    Set<String> extractedIssues = extractProjectKey(key.getProjectKey(), message);
                    for (String extractedIssue : extractedIssues)
                    {
                        String issueId = extractedIssue.toUpperCase();
                        bitbucketMapper.addChangeset(issueId, changeset);
                        coordinator.operations.get(key).setProgress(new Progress.InProgress(changeset.getRevision()));
                    }
                }
            }

            return OperationResult.YES;
        }
    }

    private final Logger logger = LoggerFactory.getLogger(DefaultSynchronizer.class);
    private final Bitbucket bitbucket;
    private final BitbucketMapper bitbucketMapper;
    private final Coordinator coordinator;

    public DefaultSynchronizer(Bitbucket bitbucket, BitbucketMapper bitbucketMapper)
    {
        this.bitbucket = bitbucket;
        this.bitbucketMapper = bitbucketMapper;

        // TODO:inject executor
        this.coordinator = new Coordinator(Executors.newFixedThreadPool(2, ThreadFactories.namedThreadFactory("BitbucketSynchronizer")));
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
        SynchronizationKey key = new SynchronizationKey(projectKey, repositoryUri);
        coordinator.operations.get(key);
    }

    public void synchronize(String projectKey, RepositoryUri repositoryUri, List<BitbucketChangeset> changesets)
    {
        SynchronizationKey key = new SynchronizationKey(projectKey, repositoryUri, changesets);
        coordinator.operations.get(key);
    }

    public Iterable<Progress> getProgress()
    {
        return coordinator.operations.values();
    }

    public Iterable<Progress> getProgress(final String projectKey, final RepositoryUri repositoryUri)
    {
        return Iterables.filter(coordinator.operations.values(), new Predicate<Progress>()
        {
            public boolean apply(Progress input)
            {
                return input.matches(projectKey, repositoryUri);
            }
        });
    }
}
