package com.atlassian.jira.plugins.dvcs.scheduler;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.sal.api.scheduling.PluginJob;

public class DvcsSchedulerJob implements PluginJob
{
    private static final Logger log = LoggerFactory.getLogger(DvcsSchedulerJob.class);

	@Override
    public void execute(Map<String, Object> jobDataMap)
    {   
		log.debug("Running DvcsSchedulerJob ");
		
		OrganizationService organizationService = (OrganizationService) jobDataMap.get("organizationService");
		RepositoryService repositoryService = (RepositoryService) jobDataMap.get("repositoryService");
		List<Organization> organizations = organizationService.getAll(false);
		for (Organization organization : organizations)
        {
			repositoryService.syncRepositoryList(organization);
        }
		
		repositoryService.removeDeletedRepositories();
    }
	
}
