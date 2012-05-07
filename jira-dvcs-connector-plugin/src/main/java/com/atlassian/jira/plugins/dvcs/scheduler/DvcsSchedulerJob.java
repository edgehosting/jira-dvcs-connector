package com.atlassian.jira.plugins.dvcs.scheduler;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.scheduling.PluginJob;

public class DvcsSchedulerJob implements PluginJob
{
    private static final Logger log = LoggerFactory.getLogger(DvcsSchedulerJob.class);

    @Override
    public void execute(Map<String, Object> jobDataMap)
    {   
        log.debug("Running DvcsSchedulerJob ");
    }

}
