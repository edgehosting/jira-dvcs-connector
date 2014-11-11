package com.atlassian.jira.plugins.dvcs.rest;

import com.atlassian.jira.plugins.dvcs.dao.SyncAuditLogDao;
import com.atlassian.jira.plugins.dvcs.rest.security.AdminOnly;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Map;
import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path ("/audit")
public class AuditRootResource
{
    private static final Logger log = LoggerFactory.getLogger(AuditRootResource.class);

    @Resource
    private SyncAuditLogDao syncAuditDao;

    @Resource
    @ComponentImport
    private TemplateRenderer templateRender;


    public AuditRootResource()
    {
        super();
    }

    @AdminOnly
    @Produces (MediaType.TEXT_HTML)
    @Path ("/repository/all/")
    @GET
    public Response showSyncAll(@QueryParam ("page") Integer page)
    {
        try
        {
            StringWriter writer = new StringWriter();
            Map<String, Object> data = MapBuilder.<String, Object>build("logs", syncAuditDao.getAll(page));
            templateRender.render("/templates/dvcs/audit/sync.vm", data, writer);
            return Response.ok(writer.toString()).build();


        }
        catch (Exception e)
        {
            log.warn("", e);
            return Response.serverError().entity("Failed to render.").build();
        }
    }

    @AdminOnly
    @Produces (MediaType.TEXT_HTML)
    @Path ("/repository/{id}/")
    @GET
    public Response showSyncByRepo(@PathParam ("id") int repoId, @QueryParam ("page") Integer page)
    {
        try
        {
            StringWriter writer = new StringWriter();
            Map<String, Object> data = MapBuilder.<String, Object>build("logs", syncAuditDao.getAllForRepo(repoId, page));
            templateRender.render("/templates/dvcs/audit/sync.vm", data, writer);
            return Response.ok(writer.toString()).build();

        }
        catch (Exception e)
        {
            log.warn("", e);
            return Response.serverError().entity("Failed to render.").build();
        }
    }

}
