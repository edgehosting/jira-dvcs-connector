package com.atlassian.jira.plugins.dvcs.scheduler;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivitySynchronizer;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.sal.api.scheduling.PluginJob;

//TODO Do we need separate scheduler for PR activities?
public class DvcsActivitySchedulerJob implements PluginJob
{
    private static final Logger log = LoggerFactory.getLogger(DvcsActivitySchedulerJob.class);

	@Override
    public void execute(Map<String, Object> jobDataMap)
    {   
		log.debug("Running DvcsSchedulerJob ");
		
		//OrganizationService organizationService = (OrganizationService) jobDataMap.get("organizationService");
		RepositoryService repositoryService = (RepositoryService) jobDataMap.get("repositoryService");
		RepositoryActivitySynchronizer activitySynchronizer = (RepositoryActivitySynchronizer) jobDataMap.get("activitySynchronizer");
		List<Repository> repositories = repositoryService.getAllRepositories();

		for (Repository repository : repositories)
        {
		    activitySynchronizer.synchronize(repository, new DefaultProgress(), true);
        }
    }
	
}
