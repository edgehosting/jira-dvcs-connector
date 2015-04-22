package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.dao.StreamCallback;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.util.concurrent.ThreadFactories;
import com.google.common.annotations.VisibleForTesting;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.Immutable;

import static com.atlassian.fugue.Option.option;
import static com.atlassian.util.concurrent.ThreadFactories.Type.DAEMON;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;

@Component
public class EventServiceImpl implements EventService
{
    private static final Logger logger = LoggerFactory.getLogger(EventServiceImpl.class);
    private static final int DESTROY_TIMEOUT_SECS = 10;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventPublisher eventPublisher;
    private final SyncEventDao syncEventDao;
    private final EventLimiterFactory eventLimiterFactory;
    private final ExecutorService eventDispatcher;

    @Autowired
    public EventServiceImpl(@ComponentImport EventPublisher eventPublisher,
            EventLimiterFactory eventLimiterFactory, SyncEventDao syncEventDao)
    {
        this(eventPublisher, syncEventDao, eventLimiterFactory, createEventDispatcher());
    }

    @VisibleForTesting
    EventServiceImpl(EventPublisher eventPublisher, SyncEventDao syncEventDao, EventLimiterFactory eventLimiterFactory, ExecutorService executorService)
    {
        this.eventPublisher = checkNotNull(eventPublisher);
        this.syncEventDao = syncEventDao;
        this.eventLimiterFactory = eventLimiterFactory;
        this.eventDispatcher = executorService;
    }

    public void storeEvent(Repository repository, SyncEvent event) throws IllegalArgumentException
    {
        storeEvent(repository, event, false);
    }

    public void storeEvent(Repository repository, SyncEvent event, boolean scheduledSync)
            throws IllegalArgumentException
    {
        storeEvent(repository.getId(), event, scheduledSync);
    }

    public void storeEvent(int repositoryId, SyncEvent event, boolean scheduledSync) throws IllegalArgumentException
    {
        try
        {
            syncEventDao.save(toSyncEventMapping(repositoryId, event, scheduledSync));
            logger.debug("Saved event for repositoryId {}: {}", repositoryId, event);
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("Can't store event (unable to convert to JSON): " + event, e);
        }
    }

    public void dispatchEvents(Repository repository)
    {
        dispatch(new DispatchRequest(repository));

    }

    @Override
    public void dispatchEvents(int repositoryId)
    {
        doDispatchEvents(new DispatchRequest(repositoryId, "repository with id " + repositoryId));
    }

    private void dispatch(final DispatchRequest dispatchRequest)
    {
        // do the event dispatching asynchronously
        eventDispatcher.submit(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                try
                {
                    doDispatchEvents(dispatchRequest);
                    return null;
                }
                catch (Throwable t)
                {
                    logger.error("Error dispatching events for: " + dispatchRequest, t);
                    throw new RuntimeException(t);
                }
            }
        });
    }

    /**
     * Shuts down the event dispatcher pool. This method waits for up to {@link #DESTROY_TIMEOUT_SECS} for the executor
     * to shut down before logging an error and returning.
     */
    @PreDestroy
    public void destroy()
    {
        destroyEventDispatcher();
    }

    /**
     * Dispatches all events for a repo. If the thread running this method is interrupted while this method is
     * dispatching events this method will return early, preserving the thread's interrupted status.
     *
     * @param dispatch a DispatchRequest
     */
    private void doDispatchEvents(final DispatchRequest dispatch)
    {
        final EventLimiter limiter = eventLimiterFactory.create();
        syncEventDao.streamAllByRepoId(dispatch.repoId(), new StreamCallback<SyncEventMapping>()
        {
            int dispatched = 0;

            @Override
            public void callback(final SyncEventMapping syncEventMapping)
            {
                if (Thread.interrupted())
                {
                    logger.error("Thread interrupted after dispatching {} events for: {}", new Object[] { dispatched, dispatch });
                    Thread.currentThread().interrupt();
                    return;
                }

                try
                {
                    final SyncEvent event = fromSyncEventMapping(syncEventMapping);
                    if (limiter.isLimitExceeded(event, option(syncEventMapping.getScheduledSync()).getOrElse(false)))
                    {
                        logger.debug("Limit exceeded, dropping event for repository {}: {}", dispatch, event);
                        return;
                    }

                    logger.debug("Publishing event for repository {}: {}", dispatch, event);
                    eventPublisher.publish(event);
                    dispatched = dispatched + 1;
                }
                catch (ClassNotFoundException e)
                {
                    logger.error("Can't dispatch event (event class not found): " + syncEventMapping.getEventClass(), e);
                }
                catch (IOException e)
                {
                    logger.error("Can't dispatch event (unable to convert from JSON): " + syncEventMapping.getEventJson(), e);
                }
                finally
                {
                    syncEventDao.delete(syncEventMapping);
                }
            }
        });

        int dropped = limiter.getLimitExceededCount();
        if (dropped > 0)
        {
            logger.info("Event limit exceeded for {}. Dropped {} subsequent events.", dispatch, dropped);
            eventPublisher.publish(new LimitExceededEvent(dropped));
        }
    }

    @Override
    public void discardEvents(Repository repository)
    {
        int deleted = syncEventDao.deleteAll(repository.getId());
        logger.debug("Deleted {} events from repo: {}", deleted, repository);
    }

    private SyncEventMapping toSyncEventMapping(int repositoryId, SyncEvent event, final Boolean scheduledSync)
            throws IOException
    {
        SyncEventMapping mapping = syncEventDao.create();
        mapping.setRepoId(repositoryId);
        mapping.setEventDate(event.getDate());
        mapping.setEventClass(event.getClass().getName());
        mapping.setEventJson(objectMapper.writeValueAsString(event));
        mapping.setScheduledSync(scheduledSync);

        return mapping;
    }

    private SyncEvent fromSyncEventMapping(SyncEventMapping mapping) throws ClassNotFoundException, IOException
    {
        final Class<?> eventClass = Class.forName(mapping.getEventClass());

        return (SyncEvent) objectMapper.readValue(mapping.getEventJson(), eventClass);
    }

    private void destroyEventDispatcher()
    {
        eventDispatcher.shutdown();

        // Unit test passes an ExecutorService with "same thread" executor, not a ThreadPoolExecutor
        if (eventDispatcher instanceof ThreadPoolExecutor)
        {
            ((ThreadPoolExecutor) eventDispatcher).getQueue().clear();
        }
        try
        {
            boolean destroyed = eventDispatcher.awaitTermination(DESTROY_TIMEOUT_SECS, SECONDS);
            if (!destroyed)
            {
                logger.error("ExecutorService did not shut down within {}s", DESTROY_TIMEOUT_SECS);
            }
        }
        catch (InterruptedException e)
        {
            logger.error("Interrupted while waiting for ExecutorService to shut down.");
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadPoolExecutor createEventDispatcher()
    {
        return ThreadPoolUtil.newSingleThreadExecutor(ThreadFactories
                        .named("DVCSConnector.EventService")
                        .type(DAEMON)
                        .build()
        );
    }

    /**
     * Parameter object for dispatching events. Makes it easier to provide a sane toString() in logging, etc.
     */
    @Immutable
    private static class DispatchRequest
    {
        private final int repoId;
        private final String repoToString;

        public DispatchRequest(@Nonnull Repository repository)
        {
            this.repoId = repository.getId();
            this.repoToString = repository.toString();
        }

        private DispatchRequest(final int repoId, final String repoToString)
        {
            this.repoId = repoId;
            this.repoToString = repoToString;
        }

        public int repoId()
        {
            return repoId;
        }

        @Override
        public String toString()
        {
            return String.format("Repository[%s]", repoToString);
        }
    }
}
