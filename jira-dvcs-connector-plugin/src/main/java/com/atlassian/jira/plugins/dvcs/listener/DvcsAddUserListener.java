package com.atlassian.jira.plugins.dvcs.listener;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.event.user.UserCreatedEvent;
import com.atlassian.crowd.model.event.UserEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.web.action.admin.UserAddedEvent;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginUninstalledEvent;
import com.atlassian.util.concurrent.ThreadFactories;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * 
 * Listens to user events (just for <code>CREATED</code> type).
 * 
 * Handler methods run asynchronously and are safe to fail. That means that it
 * does not corrupt process of adding the user because of some unexpected error
 * at this place.
 * 
 * @see #onUserAddViaInterface(UserAddedEvent)
 * @see #onUserAddViaCrowd(UserEvent)
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

    /** The event publisher. */
    private final EventPublisher eventPublisher;

    /** The organization service. */
    private final OrganizationService organizationService;

    /** The communicator provider. */
    private final DvcsCommunicatorProvider communicatorProvider;

    private final ExecutorService executorService;

    private CacheTimer cacheTimer;

    private final UserManager userManager;

    private final GroupManager groupManager;

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
    public DvcsAddUserListener(EventPublisher eventPublisher, OrganizationService organizationService,
            DvcsCommunicatorProvider communicatorProvider, UserManager userManager, GroupManager groupManager)
    {
        this.eventPublisher = eventPublisher;
        this.organizationService = organizationService;
        this.communicatorProvider = communicatorProvider;
        this.userManager = userManager;
        this.groupManager = groupManager;
        this.executorService = Executors.newFixedThreadPool(2,
                ThreadFactories.namedThreadFactory("DvcsAddUserListenerExecutorService"));

        cacheTimer = new CacheTimer(2);
        initCacheTimer();

    }

    private void initCacheTimer()
    {
        cacheTimer.scheduleOnExpiration(new OnExpiredCallback()
        {
            @Override
            public void execute(String key)
            {
                // user has been added externally
                Runnable task = new UserAddedExternallyEventProcessor(key, organizationService, communicatorProvider, userManager, groupManager);
                safeExecute(task, "Failed to handle add user externally user =  " + key+ "]");
            }
        });
    }

    /**
     * Handler method for add user via interface.
     * 
     * @param event
     *            the event object
     */
    @EventListener
    public void onUserAddViaInterface(final UserAddedEvent event)
    {
        if (event == null)
        {
            return;
        }

        // invalidate user-added-externally event record from cache
        cacheTimer.cancelSchedulerFor(event.getRequestParameters().get("username")[0]);

        String onFailMessage = "Failed to handle add user via interface event [ " + event + ", params =  "
                + event.getRequestParameters() + "] ";
        Runnable task = new UserAddedViaInterfaceEventProcessor(event, organizationService, communicatorProvider, userManager, groupManager);
        safeExecute(task, onFailMessage);
    }

    /**
     * Handler method for add user externally;
     * 
     * This event is also fired when user is added via UI
     * 
     * @param event
     *            the event object
     */
    @EventListener
    public void onUserAddViaCrowdOrInterface(/* triggered before UserAddedEvent */ UserCreatedEvent event)
    {

        // schedule invitation, only UserAddedEvent (UI event) can discard scheduler
        cacheTimer.add(event.getUser().getName());

    }

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
                executorService.submit(task);
            }
        } catch (Throwable t)
        {
            log.warn(onFailMessage, t);
        }
    }

    // @PluginEventListener
    public void onPluginDisabled(PluginDisabledEvent event)
    {
        log.info(event.getClass() + "");
        
        unregisterSelf();
    }

    @PluginEventListener
    public void onPluginUninstalled(PluginUninstalledEvent event)
    {
        log.info(event.getClass() + "");

        unregisterSelf();
    }

    private void unregisterSelf()
    {
        try
        {
            eventPublisher.unregister(this);
        } catch (Exception e)
        {
            log.warn("Failed to unregister " + this + ", cause message is " + e.getMessage());
        }
    }

    static class CacheTimer
    {

        private CacheBuilder<Object, Object> builder;
        private OnExpiredCallback onExpired;
        private Cache<String, Object> cache;

        public CacheTimer(int seconds)
        {
            super();
            this.builder = CacheBuilder.newBuilder().expireAfterWrite(seconds, TimeUnit.SECONDS);
            init(seconds);
        }

        private void init(int seconds)
        {

            RemovalListener<String, Object> listener = new RemovalListener<String, Object>()
            {
                /**
                 * i.e. invite users with default groups
                 */
                @Override
                public void onRemoval(RemovalNotification<String, Object> notification)
                {
                    if (notification.getCause() == RemovalCause.EXPIRED && onExpired != null)
                    {
                        onExpired.execute(notification.getKey());
                    }
                }
            };

            this.cache = builder.removalListener(listener).build(new CacheLoader<String, Object>()
            {
                @Override
                public String load(String key) throws Exception
                {
                    return key;
                }
            });

            // schedule cache cleanup
            int scheduleRate = seconds * 1000;

            new Timer(CacheTimer.class.getName()).scheduleAtFixedRate(new TimerTask()
            {
                @Override
                public void run()
                {
                    cache.cleanUp();
                }
            }, scheduleRate, scheduleRate);

        }

        void scheduleOnExpiration(OnExpiredCallback callback)
        {
            this.onExpired = callback;
        }

        void add(String key)
        {

            cache.getUnchecked(key);

        }

        void cancelSchedulerFor(String key)
        {
            cache.invalidate(key);
        }

    }

    static interface OnExpiredCallback
    {
        void execute(String key);
    }
    
    public void setCacheTimer(CacheTimer cacheTimer)
    {
        this.cacheTimer = cacheTimer;
    }
}