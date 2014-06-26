package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestPageMessage;
import com.google.common.collect.Iterables;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.util.Collection;
import javax.annotation.Resource;

/**
 * Message consumer for {@link com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestPageMessage}
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
public class GitHubPullRequestPageMessageConsumer implements MessageConsumer<GitHubPullRequestPageMessage>
{
    public static final String QUEUE = GitHubPullRequestPageMessageConsumer.class.getCanonicalName();
    public static final String ADDRESS = GitHubPullRequestPageMessageConsumer.class.getCanonicalName();

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.service.message.MessagingService} dependency.
     */
    @Resource
    private MessagingService messagingService;

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider} dependency.
     */
    @Resource(name = "githubClientProvider")
    private GithubClientProvider gitHubClientProvider;

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.sync.GitHubPullRequestProcessor} dependency.
     */
    @Resource
    private GitHubPullRequestProcessor gitHubPullRequestProcessor;

    @Override
    public void onReceive(final Message<GitHubPullRequestPageMessage> message, final GitHubPullRequestPageMessage payload)
    {
        Repository repository = payload.getRepository();
        int page = payload.getPage();

        PullRequestService pullRequestService = gitHubClientProvider.getPullRequestService(repository);

        PageIterator<PullRequest> pullRequestsPages = pullRequestService.pagePullRequests(RepositoryId.createFromUrl(repository.getRepositoryUrl()), "all", page, PagedRequest.PAGE_SIZE);
        Collection<PullRequest> pullRequests = Iterables.getFirst(pullRequestsPages, null);
        if (pullRequests != null)
        {
            for (PullRequest pullRequest : pullRequests)
            {
                gitHubPullRequestProcessor.processPullRequest(repository, pullRequest);
            }
        }

        if (pullRequestsPages.hasNext())
        {
            fireNextPage(message, payload);
        }
    }

    private void fireNextPage(Message<GitHubPullRequestPageMessage> message, GitHubPullRequestPageMessage payload)
    {
        GitHubPullRequestPageMessage nextMessage = new GitHubPullRequestPageMessage(payload.getProgress(), payload.getSyncAuditId(), payload.isSoftSync(), payload.getRepository(), payload.getPage() + 1);
        messagingService.publish(getAddress(), nextMessage, message.getTags());
    }

    @Override
    public String getQueue()
    {
        return QUEUE;
    }

    @Override
    public MessageAddress<GitHubPullRequestPageMessage> getAddress()
    {
        return messagingService.get(GitHubPullRequestPageMessage.class, ADDRESS);
    }

    @Override
    public int getParallelThreads()
    {
        return MessageConsumer.THREADS_PER_CONSUMER;
    }
}
