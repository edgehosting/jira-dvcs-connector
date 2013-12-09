package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.sal.api.scheduling.PluginJob;
import com.atlassian.sal.api.scheduling.PluginScheduler;

import java.util.Map;

public class BitbucketAccountsReloadJob implements PluginJob
{
    public final static String JOB_NAME = "bitbucket-accounts-reload";
    @Override
    public void execute(Map<String, Object> stringObjectMap)
    {
        BitbucketAccountsConfigService accountsConfigService = (BitbucketAccountsConfigService) stringObjectMap.get("bitbucketAccountsConfigService");
        PluginScheduler pluginScheduler = (PluginScheduler) stringObjectMap.get("pluginScheduler");

        // "unschedule" should happen at first, 
        // because an error can happened after that and job will be never unscheduled
        pluginScheduler.unscheduleJob(JOB_NAME);
        accountsConfigService.reload();
    }
}
