package com.atlassian.jira.plugins.dvcs.rest;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("/")
public class RootResource
{

    @Context
    UriInfo uriInfo;

    
    private final OrganizationService organizationService;
    
   
    private final Logger log = LoggerFactory.getLogger(RootResource.class);

    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
//    private final Synchronizer synchronizer;

	private final RepositoryService repositoryService;

	public RootResource(PermissionManager permissionManager,
						JiraAuthenticationContext jiraAuthenticationContext,
						//Synchronizer synchronizer,
						OrganizationService organizationService,
						RepositoryService repositoryService)
    {
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
//        this.synchronizer = synchronizer;
		this.organizationService = organizationService;
		this.repositoryService = repositoryService;
    }


    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repositories/")
    public Response getAllRepositories()
    {
        return Response.noContent().build();
    }

 
    @AnonymousAllowed
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repository/{id}/sync")
    public Response startRepositorySync(@PathParam("id") int id, @FormParam("payload") String payload)
    {
        log.debug("Rest request to sync repository [{}] with payload [{}]", id, payload);
        // ...
        // redirect to Repository resource - that will contain sync message/status
        UriBuilder ub = uriInfo.getBaseUriBuilder();
        URI uri = ub.path("/repository/{id}").build(id);
        return Response.seeOther(uri).build();
    }


    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/accountInfo")
    public Response accountInfo(@QueryParam("server") String server, @QueryParam("account") String account)
    {
        AccountInfo accountInfo = organizationService.getAccountInfo(server, account);
        if (accountInfo!=null) {
        	return Response.ok(accountInfo).build();
        }
        else {
        	return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
    
    

}