package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.CustomPullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestPageMessage;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.client.PageIterator;
import org.hamcrest.FeatureMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.spi.github.CustomPullRequestService.DIRECTION_DESC;
import static com.atlassian.jira.plugins.dvcs.spi.github.CustomPullRequestService.SORT_UPDATED;
import static com.atlassian.jira.plugins.dvcs.spi.github.CustomPullRequestService.STATE_ALL;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GitHub pull requests synchronization test
 */
public class GitHubPullRequestPageMessageConsumerTest
{
    public static final int DEFAULT_PAGELEN = 30;
    public static final int DEFAULT_PAGE = 1;
    @InjectMocks
    private GitHubPullRequestPageMessageConsumer testedClass;

    @Mock
    private Message<GitHubPullRequestPageMessage> message;

    @Mock
    private CustomPullRequestService gitHubPullRequestService;

    @Mock
    private Repository repository;

    @Mock
    private PullRequest pullRequest;

    @Mock
    private Progress progress;

    @Mock
    private GithubClientProvider gitHubClientProvider;

    @Mock
    private GitHubPullRequestProcessor gitHubPullRequestProcessor;

    @Mock
    private MessagingService messagingService;

    @BeforeMethod
    private void init() throws IOException
    {
        MockitoAnnotations.initMocks(this);

        when(gitHubClientProvider.getPullRequestService(repository)).thenReturn(gitHubPullRequestService);
    }

    @Test
    public void nullProcessedPRsAreHandledGracefully() throws IOException
    {
        final Long[] prIds = new Long[] {1L, 2L, 3L};

        final PageIterator<PullRequest> prIterator = mockPageIterator(ImmutableList.copyOf(prIds));
        when(gitHubPullRequestService.pagePullRequests(any(IRepositoryIdProvider.class), eq(STATE_ALL), eq(SORT_UPDATED), eq(DIRECTION_DESC), eq(DEFAULT_PAGE), eq(DEFAULT_PAGELEN)))
                .thenReturn(prIterator);
        when(gitHubPullRequestProcessor.processPullRequestIfNeeded(eq(repository), any(PullRequest.class))).thenReturn(true);

        testedClass.onReceive(message, mockPayload(true, DEFAULT_PAGE, DEFAULT_PAGELEN, null));

        verify(gitHubPullRequestProcessor, times(3)).processPullRequestIfNeeded(eq(repository), any(PullRequest.class));
        // next msg is fired
        verify(messagingService).publish(any(MessageAddress.class), matchMessageWithProcessedPrIds(prIds), any(String[].class));
    }

    private GitHubPullRequestPageMessage matchMessageWithProcessedPrIds(Long... prIds)
    {
        return argThat(new FeatureMatcher<GitHubPullRequestPageMessage, Set<Long>>(containsInAnyOrder(prIds), "processed pr ids", "processedPRIds") {

            @Override
            protected Set<Long> featureValueOf(final GitHubPullRequestPageMessage actual)
            {
                return actual.getProcessedPullRequests();
            }
        });
    }

    private PageIterator<PullRequest> mockPageIterator(final ImmutableList<Long> prIds)
    {
        PageIterator<PullRequest> prIterator = mock(PageIterator.class);
        when(prIterator.hasNext()).thenReturn(true, true, false);
        final List<PullRequest> prs = Lists.transform(prIds, new Function<Long, PullRequest>() {
            @Override
            public PullRequest apply(@Nullable final Long id)
            {
                return mockPR(id);
            }
        });
        when(prIterator.next()).thenReturn(prs);
        when(prIterator.iterator()).thenReturn(prIterator);
        return prIterator;
    }

    private PullRequest mockPR(final long id)
    {
        PullRequest pr = mock(PullRequest.class);
        when(pr.getId()).thenReturn(id);
        return pr;
    }

    private GitHubPullRequestPageMessage mockPayload(final boolean softSync, final int page, final int pagelen, final Set<Long> processedPullRequests)
    {
        GitHubPullRequestPageMessage payload = mock(GitHubPullRequestPageMessage.class);

        when(payload.getProgress()).thenReturn(progress);
        when(payload.getRepository()).thenReturn(repository);
        when(payload.getPage()).thenReturn(page);
        when(payload.getPagelen()).thenReturn(pagelen);
        when(payload.isSoftSync()).thenReturn(softSync);
        when(payload.getProcessedPullRequests()).thenReturn(processedPullRequests);

        return payload;
    }
}
