package com.atlassian.jira.plugins.dvcs.spi.github.service.impl;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
import org.eclipse.egit.github.core.service.EventService;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubEventDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubEvent;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * Implementation of the {@link GitHubEventService}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubEventServiceImpl implements GitHubEventService
{

    /**
     * @see #GitHubEventServiceImpl(GitHubEventDAO, ActiveObjects)
     */
    private final GitHubEventDAO gitHubEventDAO;

    private final GitHubEventProcessorAggregator<EventPayload> gitHubEventProcessorAggregator;

    /**
     * @see #GitHubEventServiceImpl(GitHubEventDAO, ActiveObjects)
     */
    private final ActiveObjects activeObjects;

    private final GithubClientProvider githubClientProvider;

    /**
     * Constructor.
     * 
     * @param gitHubEventDAO
     *            injected {@link GitHubEventDAO} dependency
     * @param gitHubEventProcessorAggregator
     *            injected {@link GitHubEventProcessorAggregator} dependency
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param githubClientProvider
     *            injected {@link GithubClientProvider} dependency
     */
    public GitHubEventServiceImpl(
    //
            GitHubEventDAO gitHubEventDAO, //
            GitHubEventProcessorAggregator<EventPayload> gitHubEventProcessorAggregator, //
            ActiveObjects activeObjects, //
            @Qualifier("githubClientProvider") GithubClientProvider githubClientProvider //
    )
    {
        this.gitHubEventDAO = gitHubEventDAO;
        this.activeObjects = activeObjects;
        this.gitHubEventProcessorAggregator = gitHubEventProcessorAggregator;
        this.githubClientProvider = githubClientProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubEvent gitHubEvent)
    {
        gitHubEventDAO.save(gitHubEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubEvent getByGitHubId(String gitHubId)
    {
        return gitHubEventDAO.getByGitHubId(gitHubId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubEvent getLast(GitHubRepository gitHubRepository)
    {
        return gitHubEventDAO.getLast(gitHubRepository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubEvent getLastSavePoint(GitHubRepository gitHubRepository)
    {
        return gitHubEventDAO.getLastSavePoint(gitHubRepository);
    }

    public void synchronize(final Repository domainRepository, final GitHubRepository domain)
    {
        EventService eventService = githubClientProvider.getEventService(domainRepository);

        // gets repository ID
        RepositoryId forRepositoryId = RepositoryId.createFromUrl(domainRepository.getRepositoryUrl());

        final GitHubEvent savePoint = getLastSavePoint(domain);

        // goes over events
        Iterator<Collection<Event>> eventsIterator = eventService.pageEvents(forRepositoryId).iterator();
        while (eventsIterator.hasNext())
        {
            final Collection<Event> nextPage = eventsIterator.next();

            // process whole page in one transaction
            activeObjects.executeInTransaction(new TransactionCallback<Void>()
            {

                @Override
                public Void doInTransaction()
                {
                    for (Event event : nextPage)
                    {
                        // before, not before or equals - there can exists several events with the same timestamp, but it does not mean that
                        // all of them was already proceed
                        if (savePoint != null && event.getCreatedAt().before(savePoint.getCreatedAt()))
                        {
                            // was all previous records proceed?
                            break;

                        } else if (getByGitHubId(event.getId()) != null)
                        {
                            // was already proceed? maybe partial synchronization, and there can exist remaining events for processing
                            continue;

                        }

                        gitHubEventProcessorAggregator.process(domainRepository, domain, event);

                        // saves proceed GitHub event
                        GitHubEvent gitHubEvent = getByGitHubId(event.getId());
                        if (gitHubEvent == null)
                        {
                            gitHubEvent = new GitHubEvent();
                        }
                        gitHubEvent.setGitHubId(event.getId());
                        gitHubEvent.setCreatedAt(event.getCreatedAt());
                        gitHubEvent.setDomain(domain);
                        save(gitHubEvent);
                    }

                    return null;
                }
            });

        }

        // marks new save point
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                GitHubEvent last = getLast(domain);
                if (last != null)
                {
                    last.setSavePoint(true);
                    save(last);
                }

                return null;
            }

        });
    }

}
