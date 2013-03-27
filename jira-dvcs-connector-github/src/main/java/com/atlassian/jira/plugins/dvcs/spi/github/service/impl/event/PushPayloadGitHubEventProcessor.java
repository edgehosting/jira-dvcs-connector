package com.atlassian.jira.plugins.dvcs.spi.github.service.impl.event;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.PushPayload;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPushService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubRepositoryService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubUserService;

/**
 * Implementation of the {@link GitHubEventProcessor} for the {@link PushPayload} based event.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class PushPayloadGitHubEventProcessor extends AbstractGitHubEventProcessor<PushPayload>
{

    /**
     * @see #PushPayloadGitHubEventProcessor(GitHubPushService, GitHubUserService, GitHubRepositoryService)
     */
    private final GitHubPushService gitHubPushService;

    /**
     * 
     */
    private final GitHubCommitService gitHubCommitService;

    /**
     * @see #PushPayloadGitHubEventProcessor(GitHubPushService, GitHubUserService, GitHubRepositoryService)
     */
    private final GitHubUserService gitHubUserService;

    /**
     * @see #PushPayloadGitHubEventProcessor(GitHubPushService, GitHubUserService, GitHubRepositoryService)
     */
    private final GitHubRepositoryService gitHubRepositoryService;

    /**
     * Constructor.
     * 
     * @param gitHubPushService
     *            injected {@link GitHubPushService} dependency
     * @param gitHubCommitService
     *            injected {@link GitHubCommitService} dependency
     * @param gitHubUserService
     *            injected {@link GitHubUserService} dependency
     * @param gitHubRepositoryService
     *            injected {@link GitHubRepositoryService} dependency
     */
    public PushPayloadGitHubEventProcessor(GitHubPushService gitHubPushService, GitHubCommitService gitHubCommitService,
            GitHubUserService gitHubUserService, GitHubRepositoryService gitHubRepositoryService)
    {
        this.gitHubPushService = gitHubPushService;
        this.gitHubCommitService = gitHubCommitService;
        this.gitHubUserService = gitHubUserService;
        this.gitHubRepositoryService = gitHubRepositoryService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository domainRepository, GitHubRepository domain, Event event)
    {
        PushPayload pushPayload = getPayload(event);
        GitHubUser createdBy = gitHubUserService.fetch(domainRepository, domain, event.getActor().getLogin());

        String repositoryOwner = RepositoryId.createFromUrl(event.getRepo().getUrl()).getOwner();
        String repositoryName = event.getRepo().getName();
        GitHubRepository repository = gitHubRepositoryService.fetch(domainRepository, repositoryOwner, repositoryName, event.getRepo()
                .getId());

        GitHubPush gitHubPush = gitHubPushService.getByHead(repository, pushPayload.getHead());
        if (gitHubPush == null)
        {
            gitHubPush = new GitHubPush();
        }
        gitHubPush.setDomain(domain);
        gitHubPush.setRepository(repository);
        gitHubPush.setCreatedAt(event.getCreatedAt());
        gitHubPush.setCreatedBy(createdBy);
        gitHubPush.setBefore(pushPayload.getBefore());
        gitHubPush.setHead(pushPayload.getHead());
        gitHubPush.setRef(pushPayload.getRef());

        gitHubPush.getCommits().clear();
        for (Commit commit : pushPayload.getCommits())
        {
            GitHubCommit gitHubCommit = gitHubCommitService.fetch(domainRepository, domain, repository, commit.getSha());
            gitHubPush.getCommits().add(gitHubCommit);
        }

        gitHubPushService.save(gitHubPush);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PushPayload> getEventPayloadType()
    {
        return PushPayload.class;
    }

}
