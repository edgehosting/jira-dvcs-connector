package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.UserWithAttributes;
import com.atlassian.crowd.event.user.UserAttributeStoredEvent;
import com.atlassian.crowd.exception.OperationNotPermittedException;
import com.atlassian.crowd.exception.runtime.OperationFailedException;
import com.atlassian.crowd.exception.runtime.UserNotFoundException;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.web.action.admin.UserAddedEvent;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsAddUserAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.user.util.UserManager;
import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * 
 * Listens to user events (just for <code>CREATED</code> type).
 * 
 * Handler methods run asynchronously and are safe to fail. That means that it
 * does not corrupt process of adding the user because of some unexpected error
 * at this place.
 * 
 * @see #onUserAddViaInterface(UserAddedEvent)
 * @see #onUserAttributeStore(UserAttributeStoredEvent)
 * 
 * <br />
 * <br />
 *      Created on 21.6.2012, 14:07:34 <br />
 * <br />
 * @author jhocman@atlassian.com
 * 
 */
public class DvcsAddUserListener
{

    /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(DvcsAddUserListener.class);

    private static final String UI_USER_INVITATIONS_PARAM_NAME = "com.atlassian.jira.dvcs.invite.groups";

    /** BBC-957: Attribute key to recognise Service Desk Customers during user creation */
    private static final String SERVICE_DESK_CUSTOMERS_ATTRIBUTE_KEY = "synch.servicedesk.requestor";
    
    /** The event publisher. */
    private final EventPublisher eventPublisher;

    /** The organization service. */
    private final OrganizationService organizationService;

    /** The communicator provider. */
    private final DvcsCommunicatorProvider communicatorProvider;

    private final UserManager userManager;

    private final GroupManager groupManager;
    
    private final CrowdService crowd;

    /**
     * The Constructor.
     * 
     * @param eventPublisher
     *            the event publisher
     * @param organizationService
     *            the organization service
     * @param communicatorProvider
     *            the communicator provider
     */
    public DvcsAddUserListener(EventPublisher eventPublisher,
                               OrganizationService organizationService,
                               DvcsCommunicatorProvider communicatorProvider,
                               UserManager userManager,
                               GroupManager groupManager,
                               CrowdService crowd)
    {
        this.eventPublisher = eventPublisher;
        this.organizationService = organizationService;
        this.communicatorProvider = communicatorProvider;
        this.userManager = userManager;
        this.groupManager = groupManager;
        this.crowd = crowd;
        
    }
    
    //---------------------------------------------------------------------------------------
    // Handler methods
    //---------------------------------------------------------------------------------------

    @EventListener
    public void onUserAddViaInterface(final UserAddedEvent event) 
    {
        if (event == null)
        {
            return;
        }
        
        try
        {
            log.debug("Running onUserAddViaInterface ...");
            
            String username = event.getRequestParameters().get("username")[0];
            String[] organizationIdsAndGroupSlugs = event.getRequestParameters().get(
                    UserAddedViaInterfaceEventProcessor.ORGANIZATION_SELECTOR_REQUEST_PARAM);
      
            ApplicationUser user = userManager.getUserByName(username);

            String userInvitations;
            if (organizationIdsAndGroupSlugs != null)
            {
            	userInvitations = Joiner.on(
	                    UserAddedViaInterfaceEventProcessor.ORGANIZATION_SELECTOR_REQUEST_PARAM_JOINER).join(
	                    organizationIdsAndGroupSlugs);
            	eventPublisher.publish(new DvcsAddUserAnalyticsEvent());
            } else
            {
            	// setting blank String to be sure that the crowd will not return null 
            	// https://sdog.jira.com/browse/BBC-432
            	userInvitations = " ";
            }
            
            crowd.setUserAttribute(
                    ApplicationUsers.toDirectoryUser(user),
                    UI_USER_INVITATIONS_PARAM_NAME,
                    Collections.singleton(userInvitations)
                    );
       
        } catch (UserNotFoundException e)
        {
            log.warn("UserNotFoundException : " + e.getMessage());
        } catch (OperationFailedException e)
        {
            log.warn("OperationFailedException : " + e.getMessage());
        } catch (OperationNotPermittedException e)
        {
            log.warn("OperationNotPermittedException : " + e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected exception " + e.getClass() +  " : " + e.getMessage());
        }

    }
   
    /**
     * This way we are handling the google user from studio which has not been activated yet.
     * They will get Bitbucket invitation after the first successful login.
     *
     * @param event the event
     */
    @SuppressWarnings("rawtypes")
    @EventListener
    public void onUserAttributeStore(final UserAttributeStoredEvent event)
    {

        if (event.getUser() == null)
        {
            return;
        }
        
        safeExecute(new Runnable()
        {
            @Override
            public void run()
            {
                Set attributeNames = event.getAttributeNames();
                String loginCountAttName = "login.count";

                if (attributeNames != null && 
                    attributeNames.contains(loginCountAttName) && attributeNames.size() == 1)
                {

                    Set<String> count = event.getAttributeValues(loginCountAttName);
                    log.debug("Got {} as the 'login.count' values.", count);

                    Iterator<String> countValueIterator = count.iterator();
                    if (!countValueIterator.hasNext()) 
                    {
                        return;
                    }
                    int loginCount = NumberUtils.toInt(countValueIterator.next());

                    // do the invitation for the first time login
                    if (loginCount == 1)
                    {

                        firstTimeLogin(event);

                    }
                }
            }
        }, "Failed to properly handle event " + event + " for user " + event.getUser().getName());

    }

    private void firstTimeLogin(final UserAttributeStoredEvent event)
    {
        String user = event.getUser().getName();
        UserWithAttributes attributes = crowd.getUserWithAttributes(user);

        String uiChoice = attributes.getValue(UI_USER_INVITATIONS_PARAM_NAME);
        log.debug("UI choice for user " + event.getUser().getName() + " : " + uiChoice);


        // BBC-957: ignore Service Desk Customers when processing the event.
        boolean isServiceDeskRequestor = Boolean.toString(true).equals(attributes.getValue(SERVICE_DESK_CUSTOMERS_ATTRIBUTE_KEY));

        if(!isServiceDeskRequestor)
        {
            if (uiChoice == null)
            {
                // created by NON UI mechanism, e.g. google user
                new UserAddedExternallyEventProcessor(user, organizationService, communicatorProvider, userManager,
                        groupManager).run();

            } else /* something has been chosen from UI */if (StringUtils.isNotBlank(uiChoice))
            {
                new UserAddedViaInterfaceEventProcessor(uiChoice, ApplicationUsers.from(event.getUser()), organizationService,
                        communicatorProvider, userManager, groupManager).run();
            }
        }
    }
    
    //---------------------------------------------------------------------------------------
    // Handler methods end
    //---------------------------------------------------------------------------------------

    /**
     * Wraps executorService.submit(task) method invocation with
     * <code>try-catch</code> block to ensure that no exception is propagated
     * up.
     * 
     * @param task
     *            the task
     * @param onFailMessage
     *            the on fail message
     */
    private void safeExecute(Runnable task, String onFailMessage)
    {
        try
        {
            if (task != null)
            {
               task.run();
            }
        } catch (Throwable t)
        {
            log.warn(onFailMessage, t);
        }
    }

    private void unregisterSelf()
    {
        try
        {
            eventPublisher.unregister(this);
            log.info("Listener unregistered ...");
        } catch (Exception e)
        {
            log.warn("Failed to unregister " + this + ", cause message is " + e.getMessage(), e);
        }
    }

    public void unregister() throws Exception
    {
        log.info("Attempting to unregister listener ... ");
        unregisterSelf();
    }

    public void register() throws Exception
    {
        log.info("Attempting to register listener ... ");
        
        eventPublisher.register(this);

    }

}
