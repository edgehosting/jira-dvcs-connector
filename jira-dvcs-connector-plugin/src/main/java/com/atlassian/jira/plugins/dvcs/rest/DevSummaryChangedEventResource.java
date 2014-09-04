package com.atlassian.jira.plugins.dvcs.rest;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.service.admin.DevSummaryCachePrimingStatus;
import com.atlassian.jira.plugins.dvcs.service.admin.DevSummaryChangedEventServiceImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

/**
 * REST resource for generating dev summary changed events
 */
@Path ("/event/dev-summary-changed")
public class DevSummaryChangedEventResource
{
    @Resource
    private DevSummaryChangedEventServiceImpl devSummaryChangedEventService;

    private final FeatureManager featureManager;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext authenticationContext;

    public DevSummaryChangedEventResource(final FeatureManager featureManager, final PermissionManager permissionManager, final JiraAuthenticationContext authenticationContext)
    {
        this.featureManager = featureManager;
        this.permissionManager = permissionManager;
        this.authenticationContext = authenticationContext;
    }

    @Produces (MediaType.TEXT_PLAIN)
    @POST
    public Response startPriming()
    {
        if (!permissionManager.hasPermission(Permissions.SYSTEM_ADMIN, authenticationContext.getUser()))
        {
            return response(Status.UNAUTHORIZED, null);
        }

        if (!featureManager.isOnDemand())
        {
            return response(FORBIDDEN, "Only available on Cloud instances");
        }

        if (devSummaryChangedEventService.primeDevSummaryCache())
        {
            return Response.status(Status.OK).entity("priming is scheduled").build();
        }
        else
        {
            return Response.status(Status.CONFLICT).entity("priming is already scheduled, either wait for completion or stop it").build();
        }
    }

    @Produces (MediaType.TEXT_PLAIN)
    @DELETE
    public Response stopPriming()
    {
        if (!permissionManager.hasPermission(Permissions.SYSTEM_ADMIN, authenticationContext.getUser()))
        {
            return response(Status.UNAUTHORIZED, null);
        }

        devSummaryChangedEventService.stopPriming();
        return Response.status(Status.OK).entity("Stopped Priming").build();
    }

    @Produces (MediaType.APPLICATION_JSON)
    @GET
    public Response primingStatus()
    {
        if (!permissionManager.hasPermission(Permissions.SYSTEM_ADMIN, authenticationContext.getUser()))
        {
            return response(Status.UNAUTHORIZED, null);
        }

        DevSummaryCachePrimingStatus status = devSummaryChangedEventService.getPrimingStatus();
        return Response.status(Status.OK).entity(status).build();
    }

    private Response response(@Nonnull final Status status, @Nullable final Object body)
    {
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);
        return Response
                .status(status)
                .entity(body)
                .cacheControl(cacheControl)
                .build();
    }

}
