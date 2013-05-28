package com.atlassian.jira.plugins.dvcs.scheduler;

import java.util.Map;

import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.sal.api.scheduling.PluginJob;

/**
 * A job, which is responsible to clean database from unnecessary data.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class CleaningSchedulerJob implements PluginJob
{

    /**
     * Key of job data for {@link RepositoryService}.
     */
    public static final String JOB_DATA_REPOSITORY_SERVICE = "repositoryService";

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Map<String, Object> jobDataMap)
    {
        RepositoryService repositoryService = (RepositoryService) jobDataMap.get(JOB_DATA_REPOSITORY_SERVICE);
        repositoryService.removeOrphanRepositories();
    }
}
