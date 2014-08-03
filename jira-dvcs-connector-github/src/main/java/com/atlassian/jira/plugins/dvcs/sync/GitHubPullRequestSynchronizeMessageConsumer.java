package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import javax.annotation.Resource;

/**
 * Message consumer {@link GitHubPullRequestSynchronizeMessage}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestSynchronizeMessageConsumer implements MessageConsumer<GitHubPullRequestSynchronizeMessage>
{

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPullRequestSynchronizeMessageConsumer.class);

    /**
     * @see #getQueue()
     */
    public static final String QUEUE = GitHubPullRequestSynchronizeMessageConsumer.class.getCanonicalName();

    /**
     * @see #getAddress()
     */
    public static final String ADDRESS = GitHubPullRequestSynchronizeMessage.class.getCanonicalName();

    /**
     * Injected {@link MessagingService} dependency.
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getQueue()
    {
        return QUEUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(Message<GitHubPullRequestSynchronizeMessage> message, GitHubPullRequestSynchronizeMessage payload)
    {
        Repository repository = payload.getRepository();

        PullRequest remotePullRequest = getRemotePullRequest(repository, payload.getPullRequestNumber());

        gitHubPullRequestProcessor.processPullRequest(repository, remotePullRequest);
    }

    /**
     * Loads remote {@link PullRequest}.
     *
     * @param repository
     *            owner of pull request
     * @param number
     *            number of pull request
     * @return remote pull request
     */
    private PullRequest getRemotePullRequest(Repository repository, int number)
    {
        try
        {
            PullRequestService pullRequestService = gitHubClientProvider.getPullRequestService(repository);
            return pullRequestService.getPullRequest(RepositoryId.createFromUrl(repository.getRepositoryUrl()), number);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageAddress<GitHubPullRequestSynchronizeMessage> getAddress()
    {
        return messagingService.get(GitHubPullRequestSynchronizeMessage.class, ADDRESS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getParallelThreads()
    {
        // Only one thread - comments processing is currently not thread safe!!!
        // The same comments can be proceed over the same Pull Request - because of multiple messages over the same PR
        return 1;
    }
}
