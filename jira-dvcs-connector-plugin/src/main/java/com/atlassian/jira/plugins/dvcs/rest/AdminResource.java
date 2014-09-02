package com.atlassian.jira.plugins.dvcs.rest;

import com.atlassian.jira.plugins.dvcs.service.admin.AdministrationService;
import com.atlassian.jira.plugins.dvcs.service.admin.DevSummaryCachePrimingStatus;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path ("/admin")
public class AdminResource
{
    private static final Logger log = LoggerFactory.getLogger(AdminResource.class);

    @Resource
    private AdministrationService administrationService;

    public AdminResource()
    {
        super();
    }

    @Produces (MediaType.TEXT_HTML)
    @Path ("/event/primeStart")
    @GET
    public Response generateAllEvents()
    {
        log.info("going to call for each issue key");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        administrationService.primeDevSummaryCache();
        return Response.status(Status.OK).entity("ALL GOOD took - " + stopWatch).build();
    }

    @Produces (MediaType.TEXT_HTML)
    @Path ("/event/primeStop")
    @GET
    public Response stopPriming()
    {
        administrationService.stopPriming();
        return Response.status(Status.OK).entity("Stopped Priming").build();
    }

    @Produces (MediaType.APPLICATION_JSON)
    @Path ("/event/prime")
    @GET
    public Response primingStatus()
    {
        DevSummaryCachePrimingStatus status = administrationService.getPrimingStatus();
        return Response.status(Status.OK).entity(status).build();
    }
}
