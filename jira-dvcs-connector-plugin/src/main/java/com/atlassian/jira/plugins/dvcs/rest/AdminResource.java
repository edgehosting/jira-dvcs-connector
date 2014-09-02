package com.atlassian.jira.plugins.dvcs.rest;

import com.atlassian.jira.plugins.dvcs.service.admin.AdministrationService;
import com.atlassian.jira.plugins.dvcs.service.admin.DevSummaryCachePrimingStatus;
import com.atlassian.jira.util.concurrent.ThreadFactories;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
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

    private static final ThreadFactory THREAD_FACTORY =
            ThreadFactories.namedThreadFactory(AdminResource.class.getSimpleName());

    @Resource
    private AdministrationService administrationService;

    final ThreadLocalDelegateExecutorFactory executorFactory;

    public AdminResource(final ThreadLocalDelegateExecutorFactory executorFactory)
    {
        super();
        this.executorFactory = executorFactory;
    }

    @Produces (MediaType.TEXT_HTML)
    @Path ("/event/primeStart")
    @GET
    public Response generateAllEvents()
    {
        log.info("going to call for each issue key");

        // Create an executor that uses same threadlocal context (e.g. logged-in user) as this thread
        final Executor executor = executorFactory.createExecutor(Executors.newSingleThreadExecutor(THREAD_FACTORY));

        executor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                administrationService.primeDevSummaryCache();
                log.info("priming cache took " + stopWatch);
            }
        });
        return Response.status(Status.OK).entity("priming is scheduled").build();
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
