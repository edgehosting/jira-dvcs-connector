package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.scheduler.compat.JobHandler;
import com.atlassian.scheduler.compat.JobInfo;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Runs the scheduled job of reloading Bitbucket accounts.
 */
@Component
public class BitbucketAccountsReloadJobHandler implements JobHandler
{
    @Resource
    private BitbucketAccountsConfigService accountsConfigService;

    @Override
    public void execute(final JobInfo jobInfo)
    {
        accountsConfigService.reload();
    }
}
