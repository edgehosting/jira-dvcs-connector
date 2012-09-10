package com.atlassian.jira.plugins.dvcs.smartcommits;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.fest.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.issue.worklog.WorklogInputParametersImpl;
import com.atlassian.jira.bc.issue.worklog.WorklogService;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.WorkLogHandler;

@RunWith(MockitoJUnitRunner.class)
public class WorkLogHandlerTest
{

    @Mock
    WorklogService worklogService;

    @Captor
    ArgumentCaptor<WorklogInputParametersImpl> worklogParamsCaptor;

    WorkLogHandler handler;

    public WorkLogHandlerTest()
    {
        super();
    }

    @Before
    public void setUp()
    {
        handler = new WorkLogHandler(worklogService);
    }

    @Test
    public void testHandleCommand_ShouldLogWithSuccess()
    {

        MutableIssue sampleIssue = sampleIssue();
       
        handler.handle(sampleUser(), sampleIssue, "time", Collections.list("1h 44m"));

        verify(worklogService).validateCreate(any(JiraServiceContext.class), worklogParamsCaptor.capture());

        WorklogInputParametersImpl worklogParams = worklogParamsCaptor.getValue();

        Assert.assertEquals("1h 44m", worklogParams.getTimeSpent());
        Assert.assertEquals("", worklogParams.getComment());
        Assert.assertEquals(sampleIssue, worklogParams.getIssue());
    }
    
    @Test
    public void testHandleCommandWithComment_ShouldLogWithSuccess()
    {

        MutableIssue sampleIssue = sampleIssue();
       
        handler.handle(sampleUser(), sampleIssue, "time", Collections.list("2w 3d 1h 44m   Total work logged in !!!  "));

        verify(worklogService).validateCreate(any(JiraServiceContext.class), worklogParamsCaptor.capture());

        WorklogInputParametersImpl worklogParams = worklogParamsCaptor.getValue();

        Assert.assertEquals("2w 3d 1h 44m", worklogParams.getTimeSpent());
        Assert.assertEquals("Total work logged in !!!", worklogParams.getComment());
        Assert.assertEquals(sampleIssue, worklogParams.getIssue());
    }

    private MutableIssue sampleIssue()
    {
        MutableIssue issue = Mockito.mock(MutableIssue.class);
        return issue;
    }

    private User sampleUser()
    {
        User user = Mockito.mock(User.class);
        return user;
    }

}
