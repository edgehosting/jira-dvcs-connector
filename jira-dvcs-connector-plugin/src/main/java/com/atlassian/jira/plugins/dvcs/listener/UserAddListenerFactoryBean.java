package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class UserAddListenerFactoryBean implements InitializingBean, DisposableBean
{
    private static final Logger log = LoggerFactory.getLogger(UserAddListenerFactoryBean.class);

    @ComponentImport
    private EventPublisher eventPublisher;
    private OrganizationService organizationService;
    private DvcsCommunicatorProvider communicatorProvider;
    @ComponentImport
    private UserManager userManager;
    private GroupManager groupManager;
    @ComponentImport
    private CrowdService crowdService;

    private DvcsAddUserListener dvcsAddUserListener;

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

    public void setUserManager(UserManager userManager)
    {
        this.userManager = userManager;
    }

    public void setGroupManager(GroupManager groupManager)
    {
        this.groupManager = groupManager;
    }

    public void setCrowdService(CrowdService crowdService)
    {
        this.crowdService = crowdService;
    }

    @Override
    public void destroy() throws Exception
    {
        if (dvcsAddUserListener != null)
        {
            dvcsAddUserListener.unregister();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        try
        {
            log.info("Attempt to create and register DvcsAddUserListener listener");

            Class.forName("com.atlassian.jira.event.web.action.admin.UserAddedEvent");

            dvcsAddUserListener = new DvcsAddUserListener(eventPublisher,
                    organizationService, communicatorProvider, userManager, groupManager, crowdService);

            dvcsAddUserListener.register();

        }
        catch (ClassNotFoundException e)
        {
            // Looks like we are running JIRA 5.0 and UserAddedEvent is not available
            log.warn("UserAddedEvent not available");
        }
    }

}
