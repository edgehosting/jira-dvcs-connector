package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
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

    /*
        In theory we should be able to set a repeat interval of zero milliseconds
        to mean "run once", however SAL incorrectly rejects this value. So instead
        we set the repeat interval to such a long time that in reality the job will
        never repeat. Note that we have chosen a value that fits into the database
        column, which has size (18,0).

        In Java 7, we can punctuate this constant to make its intent clearer.
     */
    @VisibleForTesting
    static final long A_VERY_LONG_TIME_INDEED = 99999999999999999L;

    private final JobHandler jobHandler;
    private final CompatibilityPluginScheduler scheduler;

    @Autowired
    public BitbucketAccountsReloadJobSchedulerImpl(@ComponentImport final CompatibilityPluginScheduler scheduler,
            final BitbucketAccountsReloadJobHandler jobHandler)
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
            scheduler.scheduleClusteredJob(
                    JOB_ID, JOB_HANDLER_KEY, new Date(currentTimeMillis() + DELAY), A_VERY_LONG_TIME_INDEED);
        }
    }
}
