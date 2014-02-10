package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
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

import static com.atlassian.scheduler.JobRunnerResponse.success;
import static com.atlassian.scheduler.config.RunMode.RUN_ONCE_PER_CLUSTER;
import static com.atlassian.scheduler.config.Schedule.runOnce;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Runs the scheduled job of reloading Bitbucket accounts.
 */
@Component
public class BitbucketAccountsReloadJobRunner implements JobRunner
{
    private final BitbucketAccountsConfigService accountsConfigService;

    public BitbucketAccountsReloadJobRunner(final BitbucketAccountsConfigService accountsConfigService)
    {
        this.accountsConfigService = accountsConfigService;
    }

    @Override
    public JobRunnerResponse runJob(final JobRunnerRequest jobRunnerRequest)
    {
        accountsConfigService.reload();
        return success();
    }
}
