package com.atlassian.jira.plugins.dvcs.scheduler;

import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;

public class DvcsScheduler implements LifecycleAware
{
    private static final Logger log = LoggerFactory.getLogger(DvcsScheduler.class);
    private static final String JOB_NAME = DvcsScheduler.class.getName() + ":job";

    private final PluginScheduler pluginScheduler; // provided by SAL

    private final long interval = 1000L * 60;// * 60; // default job interval (1 hour)

    public DvcsScheduler(PluginScheduler pluginScheduler)
    {
        this.pluginScheduler = pluginScheduler;
    }

    @Override
    public void onStart()
    {
        log.debug("Starting DVCSConnector Scheduler Job starting");
        reschedule();
    }

    public void reschedule()
    {
        Map<String, Object> data = null;

        pluginScheduler.scheduleJob(JOB_NAME,   // unique name of the job
            DvcsSchedulerJob.class,             // class of the job
            data,                               // data that needs to be passed to the job
            new Date(),                         // the time the job is to start
            interval);                          // interval between repeats, in milliseconds
    }

}
