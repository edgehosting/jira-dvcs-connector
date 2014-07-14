package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketLinks;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.jira.plugins.dvcs.util.RepositoryPullRequestMappingMock;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.model.PullRequestStatus.DECLINED;
import static com.atlassian.jira.plugins.dvcs.model.PullRequestStatus.MERGED;
import static com.atlassian.jira.plugins.dvcs.model.PullRequestStatus.OPEN;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

@Listeners (MockitoTestNgListener.class)
public class BitbucketSynchronizeActivityMessageConsumerTest
{

    @Mock
    private RepositoryPullRequestDao repositoryPullRequestDao;

    @Mock
    private Repository repository;

    @Mock
    private RepositoryPullRequestMapping localPullRequest;

    @InjectMocks
    private BitbucketSynchronizeActivityMessageConsumer messageConsumer;

    private RepositoryPullRequestMappingMock target;
    private BitbucketPullRequest source;
    private static final String AUTHOR = "joe";
    private static final String USER = "anna";

    @BeforeMethod
    public void setUp() throws Exception
    {
        source = new BitbucketPullRequest();
        source.setAuthor(createAccount(AUTHOR));

        target = new RepositoryPullRequestMappingMock();
        when(repositoryPullRequestDao.createPullRequest()).thenReturn(target);
        when(localPullRequest.getSourceBranch()).thenReturn("source-branch");
        when(localPullRequest.getDestinationBranch()).thenReturn("dest-branch");
        when(repository.getId()).thenReturn(1);

        source.setLinks(new BitbucketLinks());
        source.getLinks().setHtml(new BitbucketLink());
        source.getLinks().getHtml().setHref("some-ref");
    }

    @Test
    public void toDaoModelPullRequest_fieldExecutedByShouldBeAuthorForPullRequestOpened()
    {
        toDaoModelPullRequest_validateFieldExecutedBy(null, OPEN, AUTHOR, AUTHOR);
    }

    @Test
    public void toDaoModelPullRequest_fieldExecutedByShouldBeUserForPullRequestMerged()
    {
        toDaoModelPullRequest_validateFieldExecutedBy(USER, MERGED, USER, AUTHOR);
    }

    @Test
    public void toDaoModelPullRequest_fieldExecutedByShouldBeUserForPullRequestDeclined()
    {
        toDaoModelPullRequest_validateFieldExecutedBy(USER, DECLINED, USER, AUTHOR);
    }

    public void toDaoModelPullRequest_validateFieldExecutedBy(String closedBy, PullRequestStatus prStatus,
            String expectedExecutedBy, String expectedAuthor)
    {
        source.setClosedBy(createAccount(closedBy));
        source.setState(prStatus.name());

        RepositoryPullRequestMapping prMapping = messageConsumer.toDaoModelPullRequest(source, repository, localPullRequest, 0);

        assertEquals(expectedExecutedBy, prMapping.getExecutedBy());
        assertEquals(expectedAuthor, prMapping.getAuthor());
    }


    private BitbucketAccount createAccount(String login)
    {
        BitbucketAccount user = new BitbucketAccount();
        user.setUsername(login);
        return user;
    }
}