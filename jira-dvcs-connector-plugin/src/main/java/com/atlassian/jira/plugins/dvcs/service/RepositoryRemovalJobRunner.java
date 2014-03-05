package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.scheduler.config.JobRunnerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import javax.annotation.Nullable;

import static com.atlassian.scheduler.JobRunnerResponse.success;

@Component
public class RepositoryRemovalJobRunner implements JobRunner
{
    public static final String REPOSITORIES_PARAMETER_KEY = "repositories";

    public static final JobRunnerKey KEY = JobRunnerKey.of(JobRunner.class.getName());

    private final RepositoryService repositoryService;

    @Autowired
    public RepositoryRemovalJobRunner(final RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
    }

    @Nullable
    @Override
    public JobRunnerResponse runJob(final JobRunnerRequest jobRunnerRequest)
    {
        @SuppressWarnings("unchecked")
        final Collection<Repository> repositories =
                (Collection<Repository>) jobRunnerRequest.getJobConfig().getParameters().get(REPOSITORIES_PARAMETER_KEY);
        for (final Repository repository : repositories)
        {
            repositoryService.remove(repository);
        }
        final String message = String.format("Removed %d repositories", repositories.size());
        return success(message);
    }
}
