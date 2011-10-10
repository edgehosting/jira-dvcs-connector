package com.atlassian.jira.plugins.bitbucket.mapper.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.bitbucket.Bitbucket;
import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.common.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.common.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.mapper.OperationResult;
import com.atlassian.jira.plugins.bitbucket.mapper.Progress;
import com.atlassian.jira.plugins.bitbucket.mapper.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.mapper.Synchronizer;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;

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
                        return new Progress(templateRenderer, from, executorService.submit(new Callable<OperationResult>()
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
        private final TemplateRenderer templateRenderer;

        public Coordinator(ExecutorService executorService, TemplateRenderer templateRenderer)
        {
            this.executorService = executorService;
            this.templateRenderer = templateRenderer;
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

            SourceControlRepository repository = globalRepositoryManager.getRepository(key.getProjectKey(), key.getRepositoryUri().getRepositoryUrl());
            Iterable<Changeset> changesets = key.getChangesets() == null ?
                    bitbucket.getChangesets(repository) :
                    key.getChangesets();

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
                        globalRepositoryManager.addChangeset(issueId, changeset);
                        coordinator.operations.get(key).inProgress(changeset.getRevision(), jiraCount);
                    }
                }
            }

            return OperationResult.YES;
        }
    }

    private final Logger logger = LoggerFactory.getLogger(DefaultSynchronizer.class);
    private final Bitbucket bitbucket;
    private final Coordinator coordinator;
	private final RepositoryManager globalRepositoryManager;

    public DefaultSynchronizer(Bitbucket bitbucket, 
                               ExecutorService executorService, TemplateRenderer templateRenderer, 
                               @Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager)
    {
        this.bitbucket = bitbucket;
		this.globalRepositoryManager = globalRepositoryManager;
        this.coordinator = new Coordinator(executorService, templateRenderer);
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

    public void synchronize(String projectKey, RepositoryUri repositoryUri)
    {
        coordinator.operations.get(new SynchronizationKey(projectKey, repositoryUri));
    }

    public void synchronize(String projectKey, RepositoryUri repositoryUri, List<Changeset> changesets)
    {
        coordinator.operations.get(new SynchronizationKey(projectKey, repositoryUri, changesets));
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
