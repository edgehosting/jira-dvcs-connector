package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.scheduler.compat.CompatibilityPluginScheduler;
import com.atlassian.scheduler.compat.JobHandlerKey;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.atlassian.jira.plugins.dvcs.util.DvcsConstants.PLUGIN_KEY;

public class DvcsScheduler implements LifecycleAware
{
    @VisibleForTesting
    static final String PROPERTY_KEY = "dvcs.connector.scheduler.interval";

    @VisibleForTesting
    static final JobHandlerKey JOB_HANDLER_KEY = JobHandlerKey.of(DvcsScheduler.class.getName());

    @VisibleForTesting
    static final String JOB_ID = DvcsScheduler.class.getName() + ":job";

    private static final Logger log = LoggerFactory.getLogger(DvcsScheduler.class);
    private static final long DEFAULT_INTERVAL = 1000L * 60 * 60; // default job interval (1 hour)

    private final CompatibilityPluginScheduler scheduler;
    private final DvcsSchedulerJob dvcsSchedulerJob;
    private final EventPublisher eventPublisher;
    private final MessagingService messagingService;

    // Three because we wait for postConstruct(), onStart(), and onPluginEnabled()
    private final AtomicInteger readyToSchedule = new AtomicInteger(3);

    public DvcsScheduler(final MessagingService messagingService, final CompatibilityPluginScheduler scheduler,
            final DvcsSchedulerJob dvcsSchedulerJob, final EventPublisher eventPublisher)
    {
        this.dvcsSchedulerJob = dvcsSchedulerJob;
        this.eventPublisher = eventPublisher;
        this.messagingService = messagingService;
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void postConstruct()
    {
        eventPublisher.register(this);
        scheduleJobIfReady();
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
            scheduleJobIfReady();
        }
    }

    public void onStart()
    {
        log.debug("LifecycleAware#onStart");
        messagingService.onStart();
        scheduleJobIfReady();
    }

    @PreDestroy
    public void destroy() throws Exception
    {
        scheduler.unregisterJobHandler(JOB_HANDLER_KEY);
        eventPublisher.unregister(this);
        log.info("DvcsScheduler job unscheduled");
    }

    private void scheduleJobIfReady()
    {
        if (readyToSchedule.decrementAndGet() != 0)
        {
            // Not ready to schedule or already scheduled
            return;
        }
        // we don't need to listen to events anymore
        eventPublisher.unregister(this);
        scheduler.registerJobHandler(JOB_HANDLER_KEY, dvcsSchedulerJob);
        if (scheduler.getJobInfo(JOB_ID) != null)
        {
            return;
        }
        final long interval = Long.getLong(PROPERTY_KEY, DEFAULT_INTERVAL);
        final long randomStartTimeWithinInterval = new Date().getTime() + (long) (new Random().nextDouble() * interval);
        final Date startTime = new Date(randomStartTimeWithinInterval);
        scheduler.scheduleClusteredJob(JOB_ID, JOB_HANDLER_KEY, startTime, interval);
        log.info("DvcsScheduler start planned at " + startTime + ", interval=" + interval);
    }
}
