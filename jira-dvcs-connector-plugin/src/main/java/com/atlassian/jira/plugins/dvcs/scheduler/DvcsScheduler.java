package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.sync.SyncConfig;
import com.atlassian.scheduler.compat.CompatibilityPluginScheduler;
import com.atlassian.scheduler.compat.JobHandlerKey;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Random;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.atlassian.jira.plugins.dvcs.scheduler.SchedulerLauncher.SchedulerLauncherJob;
import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class DvcsScheduler
{
    @VisibleForTesting
    static final JobHandlerKey JOB_HANDLER_KEY = JobHandlerKey.of(DvcsScheduler.class.getName());

    @VisibleForTesting
    static final String JOB_ID = DvcsScheduler.class.getName() + ":job";

    private static final Logger log = LoggerFactory.getLogger(DvcsScheduler.class);

    private final SyncConfig syncConfig;
    private final CompatibilityPluginScheduler scheduler;
    private final DvcsSchedulerJob dvcsSchedulerJob;
    private final MessagingService messagingService;
    private final SchedulerLauncher schedulerLauncher;

    @Autowired
    public DvcsScheduler(final MessagingService messagingService,
            final CompatibilityPluginScheduler scheduler,
            final DvcsSchedulerJob dvcsSchedulerJob,
            final SchedulerLauncher schedulerLauncher, final SyncConfig syncConfig)
    {
        this.dvcsSchedulerJob = dvcsSchedulerJob;
        this.schedulerLauncher = schedulerLauncher;
        this.messagingService = checkNotNull(messagingService);
        this.scheduler = scheduler;
        this.syncConfig = syncConfig;
    }

    @PostConstruct
    public void postConstruct()
    {
        schedulerLauncher.runWhenReady(new SchedulerLauncherJob()
        {
            @Override
            public void run()
            {
                onStart();

                log.debug("executed launcher job");
            }
        });
        log.debug("scheduled launcher job");
    }

    @PreDestroy
    public void destroy() throws Exception
    {
        scheduler.unregisterJobHandler(JOB_HANDLER_KEY);
        log.info("DvcsScheduler job handler unregistered");
    }

    @VisibleForTesting
    void onStart()
    {
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

    @VisibleForTesting
    void scheduleJob()
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
}
