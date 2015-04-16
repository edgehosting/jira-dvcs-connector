package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.issue.worklog.WorklogInputParametersImpl;
import com.atlassian.jira.bc.issue.worklog.WorklogService;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.plugins.dvcs.smartcommits.handlers.WorkLogHandler;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.MockApplicationUser;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WorkLogHandlerTest
{
    @Mock
    private WorklogService worklogService;

    @Captor
    private ArgumentCaptor<WorklogInputParametersImpl> worklogParamsCaptor;

    private WorkLogHandler handler;

    @BeforeMethod
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        new MockComponentWorker().init();
        handler = new WorkLogHandler(worklogService);
    }

    @Test
    public void testHandleCommand_ShouldLogWithSuccess()
    {
        MutableIssue sampleIssue = sampleIssue();
       
        handler.handle(sampleUser(), sampleIssue, "time", Arrays.asList("1h 44m"), null);

        verify(worklogService).validateCreate(any(JiraServiceContext.class), worklogParamsCaptor.capture());

        WorklogInputParametersImpl worklogParams = worklogParamsCaptor.getValue();

        assertThat(worklogParams.getTimeSpent()).isEqualTo("1h 44m");
        assertThat(worklogParams.getComment()).isEmpty();
        assertThat(worklogParams.getIssue()).isEqualTo(sampleIssue);
    }
    
    @Test
    public void testHandleCommandWithComment_ShouldLogWithSuccess()
    {
        MutableIssue sampleIssue = sampleIssue();
       
        handler.handle(sampleUser(), sampleIssue, "time", Arrays.asList("2w 3d 1h 44m   Total work logged in !!!  "), null);

        verify(worklogService).validateCreate(any(JiraServiceContext.class), worklogParamsCaptor.capture());

        WorklogInputParametersImpl worklogParams = worklogParamsCaptor.getValue();

        assertThat(worklogParams.getTimeSpent()).isEqualTo("2w 3d 1h 44m");
        assertThat(worklogParams.getComment()).isEqualTo("Total work logged in !!!");
        assertThat(worklogParams.getIssue()).isEqualTo(sampleIssue);
    }

    private MutableIssue sampleIssue()
    {
        return mock(MutableIssue.class);
    }

    private ApplicationUser sampleUser()
    {
        return new MockApplicationUser("user");
    }
}
