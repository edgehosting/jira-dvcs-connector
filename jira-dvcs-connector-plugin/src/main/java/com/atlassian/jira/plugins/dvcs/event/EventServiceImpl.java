package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class EventServiceImpl implements EventService
{
    private static final Logger logger = LoggerFactory.getLogger(EventServiceImpl.class);

    private final EventPublisher eventPublisher;
    private final SyncEventDao syncEventDao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public EventServiceImpl(EventPublisher eventPublisher, SyncEventDao syncEventDao)
    {
        this.eventPublisher = eventPublisher;
        this.syncEventDao = syncEventDao;
    }

    public void storeEvent(Repository repository, SyncEvent event) throws IllegalArgumentException
    {
        try
        {
            syncEventDao.save(toSyncEventMapping(repository, event));
            logger.debug("Saved event for repository {}: {}", repository, event);
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException("Can't store event (unable to convert to JSON): " + event, e);
        }
    }

    public void dispatchEvents(Repository repository)
    {
        for (SyncEventMapping syncEventMapping : syncEventDao.findAllByRepoId(repository.getId()))
        {
            try
            {
                final SyncEvent event = fromSyncEventMapping(syncEventMapping);

                logger.debug("Publishing event for repository {}: {}", repository, event);
                eventPublisher.publish(event);
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
    }

    private SyncEventMapping toSyncEventMapping(Repository repository, SyncEvent event) throws IOException
    {
        SyncEventMapping mapping = syncEventDao.create();
        mapping.setRepoId(repository.getId());
        mapping.setEventDate(event.getDate());
        mapping.setEventClass(event.getClass().getName());
        mapping.setEventJson(objectMapper.writeValueAsString(event));

        return mapping;
    }

    private SyncEvent fromSyncEventMapping(SyncEventMapping mapping) throws ClassNotFoundException, IOException
    {
        final Class<?> eventClass = Class.forName(mapping.getEventClass());

        return (SyncEvent) objectMapper.readValue(mapping.getEventJson(), eventClass);
    }
}
