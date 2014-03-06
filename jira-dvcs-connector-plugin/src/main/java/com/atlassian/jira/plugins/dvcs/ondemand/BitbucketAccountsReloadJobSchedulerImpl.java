package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.scheduler.compat.CompatibilityPluginScheduler;
import com.atlassian.scheduler.compat.JobHandler;
import com.atlassian.scheduler.compat.JobHandlerKey;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Component
public class BitbucketAccountsReloadJobSchedulerImpl implements BitbucketAccountsReloadJobScheduler
{
    @VisibleForTesting
    static final String JOB_ID = "bitbucket-accounts-reload";

    @VisibleForTesting
    static final JobHandlerKey JOB_HANDLER_KEY = JobHandlerKey.of(BitbucketAccountsReloadJobScheduler.class.getName());

    @VisibleForTesting
    // Having this delay minimises the impact of a race in the schedule method
    static final long DELAY = MILLISECONDS.convert(15, SECONDS);

    private final JobHandler jobHandler;
    private final CompatibilityPluginScheduler scheduler;

    @Autowired
    public BitbucketAccountsReloadJobSchedulerImpl(
            final CompatibilityPluginScheduler scheduler, final BitbucketAccountsReloadJobHandler jobHandler)
    {
        this.scheduler = scheduler;
        this.jobHandler = jobHandler;
    }

    @PostConstruct
    public void registerJobHandler()
    {
        scheduler.registerJobHandler(JOB_HANDLER_KEY, jobHandler);
    }

    @PreDestroy
    public void unregisterJobHandler()
    {
        scheduler.unregisterJobHandler(JOB_HANDLER_KEY);
    }

    // BitbucketAccountsReloadJobScheduler
    @Override
    public void schedule()
    {
        if (scheduler.getJobInfo(JOB_ID) == null)
        {
            scheduler.scheduleClusteredJob(JOB_ID, JOB_HANDLER_KEY, new Date(currentTimeMillis() + DELAY), 0);
        }
    }
}
