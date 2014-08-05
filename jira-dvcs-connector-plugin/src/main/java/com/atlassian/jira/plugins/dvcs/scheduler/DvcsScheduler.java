package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.sync.SyncConfig;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.scheduler.compat.CompatibilityPluginScheduler;
import com.atlassian.scheduler.compat.JobHandlerKey;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.GuardedBy;

import static com.atlassian.jira.plugins.dvcs.scheduler.DvcsScheduler.LifecycleEvent.LIFECYCLE_AWARE_ON_START;
import static com.atlassian.jira.plugins.dvcs.scheduler.DvcsScheduler.LifecycleEvent.PLUGIN_ENABLED;
import static com.atlassian.jira.plugins.dvcs.scheduler.DvcsScheduler.LifecycleEvent.POST_CONSTRUCT;
import static com.atlassian.jira.plugins.dvcs.util.DvcsConstants.PLUGIN_KEY;

public class DvcsScheduler implements LifecycleAware
{
    @VisibleForTesting
    static final JobHandlerKey JOB_HANDLER_KEY = JobHandlerKey.of(DvcsScheduler.class.getName());

    @VisibleForTesting
    static final String JOB_ID = DvcsScheduler.class.getName() + ":job";

    private static final Logger log = LoggerFactory.getLogger(DvcsScheduler.class);

    private final SyncConfig syncConfig;
    private final CompatibilityPluginScheduler scheduler;
    private final DvcsSchedulerJob dvcsSchedulerJob;
    private final EventPublisher eventPublisher;
    private final MessagingService messagingService;

    @GuardedBy("this")
    private final Set<LifecycleEvent> lifecycleEvents = EnumSet.noneOf(LifecycleEvent.class);

    public DvcsScheduler(final MessagingService messagingService, final CompatibilityPluginScheduler scheduler,
            final DvcsSchedulerJob dvcsSchedulerJob, final EventPublisher eventPublisher, final SyncConfig syncConfig)
    {
        this.dvcsSchedulerJob = dvcsSchedulerJob;
        this.eventPublisher = eventPublisher;
        this.messagingService = messagingService;
        this.scheduler = scheduler;
        this.syncConfig = syncConfig;
    }

    @PostConstruct
    public void postConstruct()
    {
        eventPublisher.register(this);
        onLifecycleEvent(POST_CONSTRUCT);
    }

    /**
     * This is received from the plugin system after the plugin is fully initialized.  It is not safe to use
     * Active Objects before this event is received.
     */
    @EventListener
    public void onPluginEnabled(final PluginEnabledEvent event)
    {
        if (PLUGIN_KEY.equals(event.getPlugin().getKey()))
        {
            onLifecycleEvent(PLUGIN_ENABLED);
        }
    }

    public void onStart()
    {
        log.debug("LifecycleAware#onStart");
        onLifecycleEvent(LIFECYCLE_AWARE_ON_START);
    }

    @PreDestroy
    public void destroy() throws Exception
    {
        scheduler.unregisterJobHandler(JOB_HANDLER_KEY);
        eventPublisher.unregister(this);
        log.info("DvcsScheduler job handler unregistered");
    }

    private void scheduleJob()
    {
        // always register the job handler, regardless of whether the job has been scheduled
        scheduler.registerJobHandler(JOB_HANDLER_KEY, dvcsSchedulerJob);
        log.info("DvcsScheduler job handler registered");

        if (scheduler.getJobInfo(JOB_ID) == null)
        {
            // only schedule the job when it's not already scheduled
            final long interval = syncConfig.scheduledSyncIntervalMillis();
            final long randomStartTimeWithinInterval = new Date().getTime() + (long) (new Random().nextDouble() * interval);
            final Date startTime = new Date(randomStartTimeWithinInterval);
            scheduler.scheduleClusteredJob(JOB_ID, JOB_HANDLER_KEY, startTime, interval);
            log.info("DvcsScheduler start planned at " + startTime + ", interval=" + interval);
        }
    }

    /**
     * The latch which ensures all of the plugin/application lifecycle progress is completed before we call
     * {@code launch()}.
     */
    private void onLifecycleEvent(final LifecycleEvent event)
    {
        log.debug("onLifecycleEvent: {}", event);
        if (isLifecycleReady(event))
        {
            log.debug("Got the last lifecycle event... Time to get started!");
            // we don't need to listen to events anymore
            eventPublisher.unregister(this);

            try
            {
                scheduleJob();
            }
            catch (Exception ex)
            {
                log.error("Unexpected error during launch", ex);
            }
            messagingService.onStart();
        }
    }

    /**
     * The event latch.
     * <p>
     * When something related to the plugin initialization happens, we call this with
     * the corresponding type of the event.  We will return {@code true} at most once, when the very last type
     * of event is triggered.  This method is {@code synchronized} because {@code EnumSet} is not
     * thread-safe and because we have multiple accesses to {@code lifecycleEvents} that need to happen
     * atomically for correct behaviour.
     * </p>
     *
     * @param event the lifecycle event that occurred
     * @return {@code true} if this completes the set of initialization-related events; {@code false} otherwise
     */
    synchronized private boolean isLifecycleReady(final LifecycleEvent event)
    {
        return lifecycleEvents.add(event) && lifecycleEvents.size() == LifecycleEvent.values().length;
    }

    /**
     * Keeps track of everything that needs to happen before we are sure that it is safe
     * to talk to all of the components we need to use, particularly the {@code SchedulerService}
     * and Active Objects.  We will not try to initialize until all of them have happened.
     */
    static enum LifecycleEvent
    {
        POST_CONSTRUCT,
        PLUGIN_ENABLED,
        LIFECYCLE_AWARE_ON_START
    }
}
