package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.scheduler.compat.JobHandler;
import com.atlassian.scheduler.compat.JobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
public class DvcsSchedulerJob implements JobHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(DvcsSchedulerJob.class);

    private final OrganizationService organizationService;
    private final RepositoryService repositoryService;
    private final ActiveObjects activeObjects;

    @Autowired
    public DvcsSchedulerJob(final OrganizationService organizationService, final RepositoryService repositoryService, final ActiveObjects activeObjects)
    {
        this.organizationService = organizationService;
        this.repositoryService = repositoryService;
        this.activeObjects = activeObjects;
    }

    @Override
    public void execute(final JobInfo jobInfo)
    {
        if (activeObjects.moduleMetaData().isDataSourcePresent())
        {
            LOG.debug("Running DvcsSchedulerJob");
            syncOrganizations();
            cleanOrphanRepositories();
        }
    }

    private void syncOrganizations()
    {
        for (final Organization organization : organizationService.getAll(false))
        {
            repositoryService.syncRepositoryList(organization);
        }
    }

    /**
     * Cleans orphan repositories - deletes repositories with no existing organization,
     * whether or not the repository deleted flag is set.
     */
    private void cleanOrphanRepositories()
    {
        final List<Repository> orphanRepositories = new LinkedList<Repository>();
        for (final Repository repository : repositoryService.getAllRepositories(true))
        {
            if (organizationService.get(repository.getOrganizationId(), false) == null)
            {
                orphanRepositories.add(repository);
            }
        }
        repositoryService.removeOrphanRepositories(orphanRepositories);
    }
}
