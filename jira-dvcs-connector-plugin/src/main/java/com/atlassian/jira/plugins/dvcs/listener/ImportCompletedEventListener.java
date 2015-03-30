package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.cache.CacheManager;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.dataimport.ImportCompletedEvent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Listen to ImportCompletedEvent (raised after a XML is imported in the Integration tests) and flushes all the caches.
 */
@Component
public class ImportCompletedEventListener
{
    private static final Logger log = LoggerFactory.getLogger(ImportCompletedEventListener.class);

    private final CacheManager cacheManager;
    private final EventPublisher eventPublisher;

    @Autowired
    public ImportCompletedEventListener(@ComponentImport final CacheManager cacheManager,
            @ComponentImport final EventPublisher eventPublisher)
    {
        this.cacheManager = cacheManager;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void registerListener()
    {
        eventPublisher.register(this);
    }

    @PreDestroy
    public void unregisterListener()
    {
        eventPublisher.unregister(this);
    }


    @EventListener
    public void onImportCompleted(final ImportCompletedEvent event)
    {
        log.debug("Flushing caches ...");
        cacheManager.flushCaches();
    }
}
