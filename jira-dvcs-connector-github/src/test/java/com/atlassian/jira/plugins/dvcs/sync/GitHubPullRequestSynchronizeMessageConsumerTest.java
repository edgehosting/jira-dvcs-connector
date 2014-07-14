package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.util.RepositoryPullRequestMappingMock;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.User;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import java.util.Date;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@Listeners (MockitoTestNgListener.class)
public class GitHubPullRequestSynchronizeMessageConsumerTest
{
    @Mock
    private RepositoryPullRequestDao repositoryPullRequestDao;

    @Mock
    private Repository repository;

    @Mock
    private RepositoryPullRequestMapping localPullRequest;

    @InjectMocks
    private GitHubPullRequestSynchronizeMessageConsumer messageConsumer;

    private RepositoryPullRequestMappingMock target;
    private PullRequest source;

    private static final String AUTHOR = "joe";
    private static final String USER = "anna";

    @BeforeMethod
    public void setup()
    {
        source = new PullRequest();
        source.setUser(createUser(AUTHOR));

        target = new RepositoryPullRequestMappingMock();
        when(repositoryPullRequestDao.createPullRequest()).thenReturn(target);
        when(localPullRequest.getSourceBranch()).thenReturn("source-branch");
        when(localPullRequest.getDestinationBranch()).thenReturn("dest-branch");
        when(repository.getId()).thenReturn(1);
    }

    @Test
    public void toDaoModelPullRequest_fieldExecutedByShouldBeAuthorForPullRequestOpened()
    {
        source.setState("open");
        RepositoryPullRequestMapping prMapping = messageConsumer.toDaoModelPullRequest(repository, source, localPullRequest);

        assertEquals(AUTHOR, prMapping.getExecutedBy());
        assertEquals(AUTHOR, prMapping.getAuthor());
    }

    @Test
    public void toDaoModelPullRequest_fieldExecutedByShouldBeUserForPullRequestMerged()
    {
        source.setMergedBy(createUser(USER));
        source.setMergedAt(new Date());
        source.setState("closed");

        RepositoryPullRequestMapping prMapping = messageConsumer.toDaoModelPullRequest(repository, source, localPullRequest);

        assertEquals(USER, prMapping.getExecutedBy());
        assertEquals(AUTHOR, prMapping.getAuthor());
    }

    @Test
    public void toDaoModelPullRequest_fieldExecutedByShouldBeUserForPullRequestDeclined()
    {
        source.setMergedBy(null);
        source.setMergedAt(null);
        source.setState("closed");

        RepositoryPullRequestMapping prMapping = messageConsumer.toDaoModelPullRequest(repository, source, localPullRequest);

        assertNull(prMapping.getExecutedBy());
        assertEquals(AUTHOR, prMapping.getAuthor());
    }

    private User createUser(String login)
    {
        User user = new User();
        user.setLogin(login);
        return user;
    }
}