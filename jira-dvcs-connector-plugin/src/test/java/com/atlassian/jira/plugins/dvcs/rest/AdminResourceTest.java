package com.atlassian.jira.plugins.dvcs.rest;


import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.service.admin.AdministrationService;
import com.atlassian.jira.plugins.dvcs.service.admin.DevSummaryCachePrimingStatus;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class AdminResourceTest
{

    @Mock
    private AdministrationService administrationService;

    @Mock
    private JiraAuthenticationContext authenticationContext;

    @Mock
    private PermissionManager permissionManager;

    @Mock
    private FeatureManager featureManager;

    private AdminResource adminResource;

    @BeforeMethod
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        adminResource = new AdminResource(featureManager, permissionManager, authenticationContext);
        ReflectionTestUtils.setField(adminResource, "administrationService", administrationService);
        when(permissionManager.hasPermission(anyInt(), any(ApplicationUser.class))).thenReturn(true);
        when(featureManager.isOnDemand()).thenReturn(true);
    }

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
    public void testStartPrimingNonAdmin()
    {
        when(permissionManager.hasPermission(anyInt(), any(ApplicationUser.class))).thenReturn(false);
        Response response = adminResource.startPriming();
        assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));
    }

    @Test
    public void testStartPrimingNonOD()
    {
        when(featureManager.isOnDemand()).thenReturn(false);
        Response response = adminResource.startPriming();
        assertThat(response.getStatus(), is(Status.FORBIDDEN.getStatusCode()));
    }

    @Test
    public void testStatus()
    {
        when(administrationService.getPrimingStatus()).thenReturn(new DevSummaryCachePrimingStatus());
        Response response = adminResource.primingStatus();
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
    }

    @Test
    public void testStatusNonAdmin()
    {
        when(permissionManager.hasPermission(anyInt(), any(ApplicationUser.class))).thenReturn(false);
        Response response = adminResource.primingStatus();
        assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));
    }

    @Test
    public void testStop()
    {

        Response response = adminResource.stopPriming();
        assertThat(response.getStatus(), is(Status.OK.getStatusCode()));
    }

    @Test
    public void testStopNonAdmin()
    {
        when(permissionManager.hasPermission(anyInt(), any(ApplicationUser.class))).thenReturn(false);
        Response response = adminResource.stopPriming();
        assertThat(response.getStatus(), is(Status.UNAUTHORIZED.getStatusCode()));
    }
}
