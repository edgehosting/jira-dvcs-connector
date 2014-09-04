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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
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
    public Response startGeneration(@FormParam ("pageSize") @DefaultValue ("100") int pageSize)
    {
        if (!permissionManager.hasPermission(Permissions.SYSTEM_ADMIN, authenticationContext.getUser()))
        {
            return response(Status.UNAUTHORIZED, null);
        }

        if (!featureManager.isOnDemand())
        {
            return response(FORBIDDEN, "Only available on Cloud instances");
        }

        if (devSummaryChangedEventService.generateDevSummaryEvents(pageSize))
        {
            return Response.status(Status.OK).entity("event generation is scheduled").build();
        }
        else
        {
            return Response.status(Status.CONFLICT).entity("event generation is already scheduled, either wait for completion or stop it").build();
        }
    }

    @Produces (MediaType.TEXT_PLAIN)
    @DELETE
    public Response stopGeneration()
    {
        if (!permissionManager.hasPermission(Permissions.SYSTEM_ADMIN, authenticationContext.getUser()))
        {
            return response(Status.UNAUTHORIZED, null);
        }

        devSummaryChangedEventService.stopGeneration();
        return Response.status(Status.OK).entity("Stopped Generation").build();
    }

    @Produces (MediaType.APPLICATION_JSON)
    @GET
    public Response generationStatus()
    {
        if (!permissionManager.hasPermission(Permissions.SYSTEM_ADMIN, authenticationContext.getUser()))
        {
            return response(Status.UNAUTHORIZED, null);
        }

        DevSummaryCachePrimingStatus status = devSummaryChangedEventService.getEventGenerationStatus();
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
