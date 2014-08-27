package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
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

    @Autowired
    public DvcsSchedulerJob(final OrganizationService organizationService, final RepositoryService repositoryService)
    {
        this.organizationService = organizationService;
        this.repositoryService = repositoryService;
    }

    @Override
    public void execute(final JobInfo jobInfo)
    {
        LOG.debug("Running DvcsSchedulerJob");
        syncOrganizations();
        cleanOrphanRepositories();
    }

    private void syncOrganizations()
    {
        for (final Organization organization : organizationService.getAll(false))
        {
            try
            {
                repositoryService.syncRepositoryList(organization);
            }
            catch (SourceControlException.UnauthorisedException e)
            {
                LOG.debug("Credential failure synching repository list for " + organization + ": " + e.getMessage());
            }
        }
    }

    /**
     * Cleans orphan repositories - repositories mark as deleted with not existing organization.
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
