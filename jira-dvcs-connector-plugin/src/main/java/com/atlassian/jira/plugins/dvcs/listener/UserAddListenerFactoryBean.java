package com.atlassian.jira.plugins.dvcs.listener;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

/**
 */
public class UserAddListenerFactoryBean implements FactoryBean
{
    private static final Logger log = LoggerFactory.getLogger(UserAddListenerFactoryBean.class);

    private EventPublisher eventPublisher;
    private OrganizationService organizationService;
    private DvcsCommunicatorProvider communicatorProvider;
    private ExecutorService executorService;
    
    public Object getObject() throws Exception
    {        
        try
        {
            Class.forName("com.atlassian.jira.event.web.action.admin.UserAddedEvent");
            DvcsAddUserListener dvcsAddUserListener = new DvcsAddUserListener(eventPublisher,
                    organizationService, communicatorProvider, executorService);
            eventPublisher.register(dvcsAddUserListener);
            return dvcsAddUserListener;
        } catch (ClassNotFoundException e)
        {
            // Looks like we are running JIRA 5.0 and UserAddedEvent is not available
            log.warn("UserAddedEvent not available");
            return null;
        }
    }

    public Class<DvcsAddUserListener> getObjectType()
    {
        return DvcsAddUserListener.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    // ------------------------------------------
    public EventPublisher getEventPublisher()
    {
        return eventPublisher;
    }
    public void setEventPublisher(EventPublisher eventPublisher)
    {
        this.eventPublisher = eventPublisher;
    }
    public OrganizationService getOrganizationService()
    {
        return organizationService;
    }
    public void setOrganizationService(OrganizationService organizationService)
    {
        this.organizationService = organizationService;
    }
    public DvcsCommunicatorProvider getCommunicatorProvider()
    {
        return communicatorProvider;
    }
    public void setCommunicatorProvider(DvcsCommunicatorProvider communicatorProvider)
    {
        this.communicatorProvider = communicatorProvider;
    }

    public ExecutorService getExecutorService()
    {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService)
    {
        this.executorService = executorService;
    }
}
