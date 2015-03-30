package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.dataimport.ImportCompletedEvent;
import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Listen to ImportCompletedEvent which is raised after a XML is imported in the Integration tests or by the system so that
 * some caches can be cleared.
 */
@Component
public class ImportCompletedEventListener
{
    private static final Logger log = LoggerFactory.getLogger(ImportCompletedEventListener.class);

    private final EventPublisher eventPublisher;
    private final OrganizationDao organizationDao;

    @Autowired
    public ImportCompletedEventListener(@ComponentImport final EventPublisher eventPublisher,
            final OrganizationDao organizationDao)
    {
        this.eventPublisher = eventPublisher;
        this.organizationDao = organizationDao;
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
        log.info("Flushing Organization cache");
        organizationDao.clearCache();
    }
}
