package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.sal.api.scheduling.PluginJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DvcsSchedulerJob implements PluginJob
{
    private static final Logger log = LoggerFactory.getLogger(DvcsSchedulerJob.class);

    @Override
    public void execute(Map<String, Object> jobDataMap)
    {
        log.debug("Running DvcsSchedulerJob ");

        OrganizationService organizationService = (OrganizationService) jobDataMap.get("organizationService");
        RepositoryService repositoryService = (RepositoryService) jobDataMap.get("repositoryService");
        syncOrganizations(organizationService, repositoryService);
        cleanOrphanRepositories(organizationService, repositoryService);
    }

    /**
     * Synchronizes all organizations.
     * 
     * @param organizationService
     * @param repositoryService
     */
    private void syncOrganizations(OrganizationService organizationService, RepositoryService repositoryService)
    {
        List<Organization> organizations = organizationService.getAll(false);
        for (Organization organization : organizations)
        {
            repositoryService.syncRepositoryList(organization);
        }
    }

    /**
     * Cleans orphan repositories - repositories mark as deleted with not existing organization.
     * 
     * @param organizationService
     * @param repositoryService
     */
    private void cleanOrphanRepositories(OrganizationService organizationService, RepositoryService repositoryService)
    {
        List<Repository> orphanRepositories = new LinkedList<Repository>();
        for (Repository repository : repositoryService.getAllRepositories(true))
        {
            if (organizationService.get(repository.getOrganizationId(), false) == null)
            {
                orphanRepositories.add(repository);
            }
        }
        repositoryService.removeOrphanRepositories(orphanRepositories);
    }

}
