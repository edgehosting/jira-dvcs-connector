package com.atlassian.jira.plugins.dvcs.rest;


import com.atlassian.jira.plugins.dvcs.service.admin.AdministrationService;
import com.atlassian.jira.plugins.dvcs.service.admin.DevSummaryCachePrimingStatus;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class AdminResourceTest
{

    @Mock
    private AdministrationService administrationService;

    @InjectMocks
    private AdminResource adminResource;

    @Test
    public void testStartPrimingSuccess()
    {

        when(administrationService.primeDevSummaryCache()).thenReturn(true);
        Response response = adminResource.startPriming();
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
    }

    @Test
    public void testStartPrimingFailure()
    {

        when(administrationService.primeDevSummaryCache()).thenReturn(false);
        Response response = adminResource.startPriming();
        assertThat(response.getStatus(), is(Status.CONFLICT.getStatusCode()));
    }

    @Test
    public void testStatus()
    {

        when(administrationService.getPrimingStatus()).thenReturn(new DevSummaryCachePrimingStatus());
        Response response = adminResource.primingStatus();
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
    }

    @Test
    public void testStop()
    {

        Response response = adminResource.stopPriming();
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
    }
}
