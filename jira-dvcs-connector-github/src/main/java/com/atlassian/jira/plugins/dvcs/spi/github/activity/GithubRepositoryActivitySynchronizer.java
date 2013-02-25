package com.atlassian.jira.plugins.dvcs.spi.github.activity;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivitySynchronizer;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ColumnNameResolverService;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubEntityMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubEventMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestActionMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestLineCommentMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPushCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPushMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubUserMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestLineCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubRepositoryService;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;

/**
 * {@link RepositoryActivitySynchronizer} implementation over GitHub repository.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GithubRepositoryActivitySynchronizer implements RepositoryActivitySynchronizer
{

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GitHubRepositoryService, GitHubEventService, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService, GitHubCommitService, RepositoryActivityDao,
     *      ColumnNameResolverService)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GitHubRepositoryService, GitHubEventService, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService, GitHubCommitService, RepositoryActivityDao,
     *      ColumnNameResolverService)
     */
    private final GitHubRepositoryService gitHubRepositoryService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GitHubRepositoryService, GitHubEventService, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService, GitHubCommitService, RepositoryActivityDao,
     *      ColumnNameResolverService)
     */
    private final GitHubEventService gitHubEventService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GitHubRepositoryService, GitHubEventService, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService, GitHubCommitService, RepositoryActivityDao,
     *      ColumnNameResolverService)
     */
    private final GitHubPullRequestService gitHubPullRequestService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GitHubRepositoryService, GitHubEventService, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService, GitHubCommitService, RepositoryActivityDao,
     *      ColumnNameResolverService)
     */
    private final GitHubPullRequestCommentService gitHubPullRequestCommentService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GitHubRepositoryService, GitHubEventService, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService, GitHubCommitService, RepositoryActivityDao,
     *      ColumnNameResolverService)
     */
    private final GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GitHubRepositoryService, GitHubEventService, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService, GitHubCommitService, RepositoryActivityDao,
     *      ColumnNameResolverService)
     */
    private final GitHubCommitService gitHubCommitService;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GitHubRepositoryService, GitHubEventService, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService, GitHubCommitService, RepositoryActivityDao,
     *      ColumnNameResolverService)
     */
    private final RepositoryActivityDao repositoryActivityDao;

    /**
     * @see #GithubRepositoryActivitySynchronizer(ActiveObjects, GitHubRepositoryService, GitHubEventService, GitHubPullRequestService,
     *      GitHubPullRequestCommentService, GitHubPullRequestLineCommentService, GitHubCommitService, RepositoryActivityDao,
     *      ColumnNameResolverService)
     */
    private final ColumnNameResolverService columnNameResolverService;

    /**
     * {@link ColumnNameResolverService#desc(Class)} of the {@link GitHubEntityMapping}.
     */
    private final GitHubEntityMapping gitHubEntityMappingDescription;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param gitHubRepositoryService
     *            injected {@link GitHubRepositoryService} dependency
     * @param gitHubEventService
     *            injected {@link GitHubEventService} dependency
     * @param gitHubPullRequestService
     *            injected {@link GitHubPullRequestService} dependency
     * @param gitHubPullRequestCommentService
     *            injected {@link GitHubPullRequestCommentService} dependency
     * @param gitHubPullRequestLineCommentService
     *            injected {@link GitHubPullRequestLineCommentService} dependency
     * @param gitHubCommitService
     *            injected {@link GitHubCommitService} dependency
     * @param repositoryActivityDao
     *            injected {@link RepositoryActivityDao} dependency
     * @param columnNameResolverService
     *            injected {@link ColumnNameResolverService} dependency
     */
    public GithubRepositoryActivitySynchronizer(//
            ActiveObjects activeObjects, //
            GitHubRepositoryService gitHubRepositoryService, //
            GitHubEventService gitHubEventService, //
            GitHubPullRequestService gitHubPullRequestService, //
            GitHubPullRequestCommentService gitHubPullRequestCommentService, //
            GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService, //
            GitHubCommitService gitHubCommitService, //
            RepositoryActivityDao repositoryActivityDao, //
            ColumnNameResolverService columnNameResolverService //
    )
    {
        this.activeObjects = activeObjects;
        this.gitHubRepositoryService = gitHubRepositoryService;
        this.gitHubEventService = gitHubEventService;
        this.gitHubPullRequestService = gitHubPullRequestService;
        this.gitHubPullRequestCommentService = gitHubPullRequestCommentService;
        this.gitHubPullRequestLineCommentService = gitHubPullRequestLineCommentService;
        this.gitHubCommitService = gitHubCommitService;
        this.repositoryActivityDao = repositoryActivityDao;

        this.columnNameResolverService = columnNameResolverService;
        gitHubEntityMappingDescription = columnNameResolverService.desc(GitHubEntityMapping.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronize(final Repository domainRepository, boolean softSync)
    {
        final GitHubRepository domain = gitHubRepositoryService.fetch(domainRepository, domainRepository.getOrgName(),
                domainRepository.getName(), 0);

        if (!softSync)
        {
            cleanAll(domainRepository, domain);
        }

        gitHubEventService.synchronize(domainRepository, domain);
        gitHubPullRequestService.synchronize(domainRepository, domain);
        for (GitHubPullRequest pullRequest : gitHubPullRequestService.getByRepository(domain))
        {
            gitHubPullRequestCommentService.synchronize(domainRepository, pullRequest);
            gitHubPullRequestLineCommentService.synchronize(domainRepository, pullRequest);
            gitHubCommitService.synchronize(domainRepository, domain, pullRequest);

            repositoryActivityDao.updatePullRequestIssueKyes(repositoryActivityDao.findRequestByRemoteId(domainRepository.getId(),
                    pullRequest.getGitHubId()).getID());
        }
    }

    /**
     * Cleans all tables.
     * 
     * @param forRepository
     * @param gitHubRepository
     */
    private void cleanAll(Repository forRepository, GitHubRepository gitHubRepository)
    {
        repositoryActivityDao.removeAll(forRepository);

        @SuppressWarnings("unchecked")
        Class<? extends GitHubEntityMapping>[] entitiesForClean = (Class<? extends GitHubEntityMapping>[]) new Class[] {
                GitHubPullRequestLineCommentMapping.class, GitHubPullRequestCommentMapping.class, GitHubPullRequestActionMapping.class,
                GitHubPullRequestCommitMapping.class, GitHubPullRequestMapping.class, GitHubPushCommitMapping.class,
                GitHubCommitMapping.class, GitHubPushMapping.class, GitHubUserMapping.class, GitHubEventMapping.class };

        for (Class<? extends GitHubEntityMapping> entityToClean : entitiesForClean)
        {
            ActiveObjectsUtils.delete(
                    activeObjects,
                    entityToClean,
                    Query.select().where(columnNameResolverService.column(gitHubEntityMappingDescription.getDomain()) + " = ? ",
                            gitHubRepository.getId()));
        }

    }
}
