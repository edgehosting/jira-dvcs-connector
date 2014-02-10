package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.SchedulerRuntimeException;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.JobRunnerKey;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.stereotype.Component;

import java.util.Date;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.atlassian.scheduler.config.RunMode.RUN_ONCE_PER_CLUSTER;
import static com.atlassian.scheduler.config.Schedule.runOnce;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Component
public class BitbucketAccountsReloadJobSchedulerImpl implements BitbucketAccountsReloadJobScheduler
{
    @VisibleForTesting
    static final JobId JOB_ID = JobId.of("bitbucket-accounts-reload");

    @VisibleForTesting
    static final JobRunnerKey JOB_RUNNER_KEY = JobRunnerKey.of(BitbucketAccountsReloadJobScheduler.class.getName());

    @VisibleForTesting
    // Having this delay minimises the impact of a race in the schedule method
    static final long DELAY = MILLISECONDS.convert(15, SECONDS);

    private static JobConfig getJobConfig(final Date runTime)
    {
        return JobConfig.forJobRunnerKey(JOB_RUNNER_KEY)
                .withRunMode(RUN_ONCE_PER_CLUSTER)
                .withSchedule(runOnce(runTime));
    }

    private final JobRunner jobRunner;
    private final SchedulerService schedulerService;

    public BitbucketAccountsReloadJobSchedulerImpl(
            final SchedulerService schedulerService, final BitbucketAccountsReloadJobRunner jobRunner)
    {
        this.schedulerService = schedulerService;
        this.jobRunner = jobRunner;
    }

    @PostConstruct
    public void registerJobRunner()
    {
        schedulerService.registerJobRunner(JOB_RUNNER_KEY, jobRunner);
    }

    @PreDestroy
    public void unregisterJobRunner()
    {
        schedulerService.unregisterJobRunner(JOB_RUNNER_KEY);
    }

    // BitbucketAccountsReloadJobScheduler
    @Override
    public void schedule()
    {
        if (schedulerService.getJobDetails(JOB_ID) != null)
        {
            // Already scheduled
            return;
        }
        try
        {
            schedulerService.scheduleJob(JOB_ID, getJobConfig(new Date(currentTimeMillis() + DELAY)));
        }
        catch (SchedulerServiceException e)
        {
            throw new SchedulerRuntimeException("Failed to schedule job", e);
        }
    }
}
