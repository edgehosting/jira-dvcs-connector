package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;

import static com.atlassian.scheduler.JobRunnerResponse.success;

@Component
public class DvcsSchedulerJob implements JobRunner
{
    private static final Logger LOG = LoggerFactory.getLogger(DvcsSchedulerJob.class);

    private final OrganizationService organizationService;
    private final RepositoryService repositoryService;

    public DvcsSchedulerJob(final OrganizationService organizationService, final RepositoryService repositoryService)
    {
        this.organizationService = organizationService;
        this.repositoryService = repositoryService;
    }


    @Nullable
    @Override
    public JobRunnerResponse runJob(final JobRunnerRequest jobRunnerRequest)
    {
        LOG.debug("Running DvcsSchedulerJob");
        syncOrganizations();
        cleanOrphanRepositories();
        return success();
    }

    private void syncOrganizations()
    {
        for (final Organization organization : organizationService.getAll(false))
        {
            repositoryService.syncRepositoryList(organization);
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
