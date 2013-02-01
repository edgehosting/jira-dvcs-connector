package com.atlassian.jira.plugins.dvcs.scheduler;

import java.util.Date;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import com.google.common.collect.Maps;

public class DvcsScheduler implements LifecycleAware
{
	private static final Logger log = LoggerFactory.getLogger(DvcsScheduler.class);
	private static final String JOB_NAME = DvcsScheduler.class.getName() + ":job";

	private final PluginScheduler pluginScheduler; // provided by SAL
	
	private static final String PROPERTY_KEY = "dvcs.connector.scheduler.interval";
	private static final long DEFAULT_INTERVAL = 1000L * 60 * 60; // default job interval (1 hour)
	private long interval = DEFAULT_INTERVAL;
	private final OrganizationService organizationService;
	private final RepositoryService repositoryService;

	public DvcsScheduler(PluginScheduler pluginScheduler, OrganizationService organizationService,
	        RepositoryService repositoryService)
	{
		this.pluginScheduler = pluginScheduler;
		this.organizationService = organizationService;
		this.repositoryService = repositoryService;
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
		data.put("organizationService",organizationService);
		data.put("repositoryService",repositoryService);
		
		long randomStartTimeWithinInterval = new Date().getTime()+nextLong(new Random(), interval);
		pluginScheduler.scheduleJob(JOB_NAME, // unique name of the job
		        DvcsSchedulerJob.class, // class of the job
		        data, // data that needs to be passed to the job
		        new Date(randomStartTimeWithinInterval), // the time the job is to start
		        interval); // interval between repeats, in milliseconds
	}

	
	/**
	 * Random long number generator where 0 < n < max
	 * http://stackoverflow.com/questions/2546078/java-random-long-number-in-0-x-n-range
	 * @param rng
	 * @param max
	 * @return
	 */
	private long nextLong(Random rng, long max) {
	    // error checking and 2^x checking removed for simplicity.
	    long bits, val;
	    do {
	       bits = (rng.nextLong() << 1) >>> 1;
	       val = bits % max;
	    } while (bits-val+(max-1) < 0L);
	    return val;
	 }
}
