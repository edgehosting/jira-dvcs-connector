package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.Date;
import java.util.Map;
import java.util.Random;

public class DvcsScheduler implements LifecycleAware, DisposableBean
{
    private static final Logger log = LoggerFactory.getLogger(DvcsScheduler.class);
    private static final String JOB_NAME = DvcsScheduler.class.getName() + ":job";

    private final PluginScheduler pluginScheduler; // provided by SAL

    private static final String PROPERTY_KEY = "dvcs.connector.scheduler.interval";
	private static final long DEFAULT_INTERVAL = 1000L * 60 * 60; // default job interval (1 hour)
    private long interval = DEFAULT_INTERVAL;
    private final OrganizationService organizationService;
    private final RepositoryService repositoryService;
    private final MessagingService messagingService;

    public DvcsScheduler(PluginScheduler pluginScheduler, OrganizationService organizationService, RepositoryService repositoryService, MessagingService messagingService)
    {
        this.pluginScheduler = pluginScheduler;
        this.organizationService = organizationService;
        this.repositoryService = repositoryService;
        this.messagingService = messagingService;
    }

    @Override
    public void onStart()
    {
        log.debug("onStart");
        this.interval = Long.getLong(PROPERTY_KEY, DEFAULT_INTERVAL);
        reschedule();
        messagingService.onStart();
    }

    public void reschedule()
    {
        Map<String, Object> data = Maps.newHashMap();
        data.put("organizationService", organizationService);
        data.put("repositoryService", repositoryService);

        long randomStartTimeWithinInterval = new Date().getTime() + (long) (new Random().nextDouble() * interval);
        Date startTime = new Date(randomStartTimeWithinInterval);

        log.info("DvcsScheduler start planned at " + startTime + ", interval=" + interval);
        pluginScheduler.scheduleJob(JOB_NAME, // unique name of the job
                DvcsSchedulerJob.class, // class of the job
                data, // data that needs to be passed to the job
                startTime, // the time the job is to start
                interval); // interval between repeats, in milliseconds
    }

    @Override
    public void destroy() throws Exception
    {
        pluginScheduler.unscheduleJob(JOB_NAME);
        log.info("DvcsScheduler job unscheduled");
    }
}
