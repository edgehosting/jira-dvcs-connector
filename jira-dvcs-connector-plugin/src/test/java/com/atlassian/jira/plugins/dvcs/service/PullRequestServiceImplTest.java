package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping.Status;
import com.atlassian.jira.plugins.dvcs.event.PullRequestUpdatedEvent;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners(MockitoTestNgListener.class)
public class PullRequestServiceImplTest
{
    static final int REPO_ID = 123;
    static final int PR_ID = 456;
    static final String NAME = "my PR";
    static final String SRC_BRANCH = "from_branch";
    static final String DST_BRANCH = "to_branch";
    static final RepositoryPullRequestMapping.Status STATUS = Status.OPEN;
    static final Date UPDATED_ON = new Date();
    static final String SOURCE_REPO = "my_repo";
    static final int COMMENT_COUNT = 23;

    @Mock
    RepositoryPullRequestDao dao;

    @Mock
    RepositoryPullRequestMapping origPr;

    @Mock
    RepositoryPullRequestMapping updatePr;

    @Mock
    ThreadEvents threadEvents;

    @Mock
    RepositoryService repositoryService;

    @Mock
    Repository repository;

    @InjectMocks
    PullRequestServiceImpl service;

    @BeforeMethod
    public void setUp() throws Exception
    {
//        MockitoAnnotations.initMocks(this);

        when(repository.getOrgHostUrl()).thenReturn("https://bitbucket.org/fusiontestaccount");
        when(repositoryService.get(REPO_ID)).thenReturn(repository);

        when(dao.findRequestById(PR_ID)).thenReturn(origPr);

        trainPullRequestMock(origPr);
        trainPullRequestMock(updatePr, origPr.getName() + "-UPDATED");

        when(dao.updatePullRequestInfo(PR_ID, updatePr.getName(), SRC_BRANCH, DST_BRANCH, STATUS, UPDATED_ON, SOURCE_REPO, COMMENT_COUNT)).thenReturn(updatePr);
    }

    @Test
    public void updatePullRequestShouldUpdatePullRequestInDatabase() throws Exception
    {
        service.updatePullRequest(PR_ID, updatePr);
        verify(dao).updatePullRequestInfo(PR_ID, updatePr.getName(), SRC_BRANCH, DST_BRANCH, STATUS, UPDATED_ON, SOURCE_REPO, COMMENT_COUNT);
    }

    @Test
    public void updatePullRequestShouldPublishEventAfterUpdating() throws Exception
    {
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        service.updatePullRequest(PR_ID, updatePr);
        verify(threadEvents).broadcast(eventCaptor.capture());
        assertThat(eventCaptor.getValue(), instanceOf(PullRequestUpdatedEvent.class));

        PullRequestUpdatedEvent updateEvent = (PullRequestUpdatedEvent) eventCaptor.getValue();
        assertThat(origPr.getName(), equalTo(updateEvent.getPullRequestBeforeUpdate().getName()));
        assertThat(updatePr.getName(), equalTo(updateEvent.getPullRequest().getName()));
    }

    private void trainPullRequestMock(final RepositoryPullRequestMapping pullRequest)
    {
        trainPullRequestMock(pullRequest, NAME);
    }

    private void trainPullRequestMock(final RepositoryPullRequestMapping pullRequest, final String name)
    {
        when(pullRequest.getName()).thenReturn(name);
        when(pullRequest.getSourceBranch()).thenReturn(SRC_BRANCH);
        when(pullRequest.getDestinationBranch()).thenReturn(DST_BRANCH);
        when(pullRequest.getLastStatus()).thenReturn(STATUS.name());
        when(pullRequest.getUpdatedOn()).thenReturn(UPDATED_ON);
        when(pullRequest.getSourceRepo()).thenReturn(SOURCE_REPO);
        when(pullRequest.getCommentCount()).thenReturn(COMMENT_COUNT);
        when(pullRequest.getToRepositoryId()).thenReturn(REPO_ID);
    }
}
