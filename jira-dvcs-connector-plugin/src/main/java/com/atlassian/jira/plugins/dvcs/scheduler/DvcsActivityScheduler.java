package com.atlassian.jira.plugins.dvcs.scheduler;

import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.sync.activity.RepositoryActivitySynchronizer;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import com.google.common.collect.Maps;

public class DvcsActivityScheduler implements LifecycleAware
{
    private static final Logger log = LoggerFactory.getLogger(DvcsActivityScheduler.class);
    private static final String JOB_NAME = DvcsActivityScheduler.class.getName() + ":job";

    private final PluginScheduler pluginScheduler; // provided by SAL

    private static final String PROPERTY_KEY = "dvcs.activity.connector.scheduler.interval";
    private static final long DEFAULT_INTERVAL = 1000L * 60 * 60; // default job
                                                                  // interval (1
                                                                  // hour)
    private long interval = DEFAULT_INTERVAL;
    private final OrganizationService organizationService;
    private final RepositoryService repositoryService;
    private final RepositoryActivitySynchronizer activitySynchronizer;

    public DvcsActivityScheduler(PluginScheduler pluginScheduler, OrganizationService organizationService,
            RepositoryService repositoryService, RepositoryActivitySynchronizer activitySynchronizer)
    {
        this.pluginScheduler = pluginScheduler;
        this.organizationService = organizationService;
        this.repositoryService = repositoryService;
        this.activitySynchronizer = activitySynchronizer;
    }

    @Override
    public void onStart()
    {
        log.debug("onStart");
        this.interval = SystemUtils.getSystemPropertyLong(PROPERTY_KEY, DEFAULT_INTERVAL);
        log.debug("Starting DVCSConnector Scheduler Job. interval=" + interval);
        reschedule();
    }

    public void reschedule()
    {
        Map<String, Object> data = Maps.newHashMap();
        data.put("organizationService", organizationService);
        data.put("repositoryService", repositoryService);
        data.put("activitySynchronizer", activitySynchronizer);

        pluginScheduler.scheduleJob(JOB_NAME, // unique name of the job
                DvcsActivitySchedulerJob.class, // class of the job
                data, // data that needs to be passed to the job
                new Date(), // the time the job is to start
                interval); // interval between repeats, in milliseconds
    }

}
