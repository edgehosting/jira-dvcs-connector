package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.scheduler.SchedulerRuntimeException;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.JobRunnerKey;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Random;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.atlassian.scheduler.config.RunMode.RUN_ONCE_PER_CLUSTER;
import static com.atlassian.scheduler.config.Schedule.forInterval;

public class DvcsScheduler
{
    @VisibleForTesting
    static final String PROPERTY_KEY = "dvcs.connector.scheduler.interval";

    @VisibleForTesting
    static final JobRunnerKey JOB_RUNNER_KEY = JobRunnerKey.of(DvcsScheduler.class.getName());

    @VisibleForTesting
    static final JobId JOB_ID = JobId.of(DvcsScheduler.class.getName() + ":job");

    private static final Logger log = LoggerFactory.getLogger(DvcsScheduler.class);
    private static final long DEFAULT_INTERVAL = 1000L * 60 * 60; // default job interval (1 hour)

    private final DvcsSchedulerJob dvcsSchedulerJobRunner;
    private final MessagingService messagingService;
    private final SchedulerService schedulerService;

    public DvcsScheduler(final MessagingService messagingService, final SchedulerService schedulerService,
            final DvcsSchedulerJob dvcsSchedulerJobRunner)
    {
        this.dvcsSchedulerJobRunner = dvcsSchedulerJobRunner;
        this.messagingService = messagingService;
        this.schedulerService = schedulerService;
    }

    @PostConstruct
    public void onStart()
    {
        log.debug("onStart");
        messagingService.onStart();
        schedulerService.registerJobRunner(JOB_RUNNER_KEY, dvcsSchedulerJobRunner);
        reschedule();
    }

    @PreDestroy
    public void destroy() throws Exception
    {
        schedulerService.unregisterJobRunner(JOB_RUNNER_KEY);
        log.info("DvcsScheduler job unscheduled");
    }

    private void reschedule()
    {
        if (schedulerService.getJobDetails(JOB_ID) != null)
        {
            // Already scheduled
            return;
        }
        final long interval = Long.getLong(PROPERTY_KEY, DEFAULT_INTERVAL);
        final long randomStartTimeWithinInterval = new Date().getTime() + (long) (new Random().nextDouble() * interval);
        final Date startTime = new Date(randomStartTimeWithinInterval);
        try
        {
            schedulerService.scheduleJob(JOB_ID, getJobConfig(startTime, interval));
            log.info("DvcsScheduler start planned at " + startTime + ", interval=" + interval);
        }
        catch (SchedulerServiceException e)
        {
            throw new SchedulerRuntimeException("Failed to schedule job", e);
        }
    }

    private JobConfig getJobConfig(final Date firstRunTime, final long intervalInMillis)
    {
        return JobConfig.forJobRunnerKey(JOB_RUNNER_KEY)
                .withRunMode(RUN_ONCE_PER_CLUSTER)
                .withSchedule(forInterval(intervalInMillis, firstRunTime));
    }
}
