package com.atlassian.jira.plugins.dvcs.rest;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.Consumes;
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
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.RepositoryList;
import com.atlassian.jira.plugins.dvcs.model.SentData;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfigService;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.webfragments.WebfragmentRenderer;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

/**
 * The Class RootResource.
 */
@Path("/")
public class RootResource
{

	/** The uri info. */
	@Context
	UriInfo uriInfo;

	/** The organization service. */
	private final OrganizationService organizationService;

	/** The log. */
	private final Logger log = LoggerFactory.getLogger(RootResource.class);

	/** The repository service. */
	private final RepositoryService repositoryService;
	
	/** The webfragment renderer. */
	private final WebfragmentRenderer webfragmentRenderer;

    private final AccountsConfigService ondemandAccountConfig;

	/**
	 * The Constructor.
	 * 
	 * @param organizationService
	 *            the organization service
	 * @param repositoryService
	 *            the repository service
	 */
	public RootResource(OrganizationService organizationService, RepositoryService repositoryService, WebfragmentRenderer webfragmentRenderer, AccountsConfigService ondemandAccountConfig)
	{
		this.organizationService = organizationService;
		this.repositoryService = repositoryService;
		this.webfragmentRenderer = webfragmentRenderer;
        this.ondemandAccountConfig = ondemandAccountConfig;
	}

	/**
	 * Gets the repository.
	 * 
	 * @param id
	 *            the id
	 * @return the repository
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/repository/{id}")
	public Response getRepository(@PathParam("id") int id)
	{
		Repository repository = repositoryService.get(id);
		
		if (repository != null) {

			return Response.ok(repository).build();
		
		} else {
			
			return Response.noContent().build();
		}

	}

	/**
	 * Gets the all repositories.
	 * 
	 * @return the all repositories
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/repositories/")
	public Response getAllRepositories()
	{
		List<Repository> activeRepositories = repositoryService.getAllRepositories();

		return Response.ok(new RepositoryList(activeRepositories)).build();
	}

	/**
	 * Start repository sync.
	 * 
	 * @param id
	 *            the id
	 * @param payload
	 *            the payload
	 * @return the response
	 */
	@AnonymousAllowed
	@POST
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/repository/{id}/sync")
	public Response startRepositorySync(@PathParam("id") int id, @FormParam("payload") String payload)
	{
		log.debug("Rest request to sync repository [{}] with payload [{}]", id, payload);

		
		repositoryService.sync(id, true);
		return Response.ok().build();
		
        /*if (payload == null)
        {
            repositoryService.sync(id, false);
        } else
        {
            repositoryService.sync(id, true);
        }

		// ...
		// redirect to Repository resource - that will contain sync
		// message/status
		UriBuilder ub = uriInfo.getBaseUriBuilder();
		URI uri = ub.path("/repository/{id}").build(id);

		return Response.seeOther(uri).build();*/
	}

	/**
	 * Account info.
	 * 
	 * @param server
	 *            the server
	 * @param account
	 *            the account
	 * @return the response
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/accountInfo")
	public Response accountInfo(@QueryParam("server") String server, @QueryParam("account") String account)
	{
		AccountInfo accountInfo = organizationService.getAccountInfo(server, account);

		if (accountInfo != null)
		{
			return Response.ok(accountInfo).build();
		} else
		{
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/organization/{id}/syncRepoList")
	public Response syncRepoList(@PathParam("id") String organizationId)
	{

		Organization organization = organizationService.get(Integer.parseInt(organizationId), false);
		repositoryService.syncRepositoryList(organization);
		return Response.noContent().build();
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/org/{id}/autolink")
	@Consumes({MediaType.APPLICATION_JSON})
	public Response enableOrganizationAutolinkNewRepos(@PathParam("id") int id, SentData autolink)
	{
		organizationService.enableAutolinkNewRepos(id, Boolean.parseBoolean(autolink.getPayload()));
		return Response.noContent().build();
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/org/{id}/globalsmarts")
	@Consumes({MediaType.APPLICATION_JSON})
	public Response enableSmartcommitsOnNewRepos(@PathParam("id") int id,
			SentData autoinvite)
	{
		organizationService.enableSmartcommitsOnNewRepos(id, Boolean.parseBoolean(autoinvite.getPayload()));
		return Response.noContent().build();
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/repo/{id}/autolink")
	@Consumes({MediaType.APPLICATION_JSON})
	public Response enableRepositoryAutolink(@PathParam("id") int id, SentData autolink)
	{
		repositoryService.enableRepository(id, Boolean.parseBoolean(autolink.getPayload()));
		return Response.noContent().build();
	}
	
	@POST
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/repo/{id}/smart")
	@Consumes({MediaType.APPLICATION_JSON})
	public Response enableSmartcommits(@PathParam("id") int id, SentData enabled)
	{
		// todo handle exceptions
		repositoryService.enableRepositorySmartcommits(id, Boolean.parseBoolean(enabled.getPayload()));
		return Response.noContent().build();
	}
	
	@GET
	@Produces({ MediaType.TEXT_HTML })
	@Path("/fragment/{id}/defaultgroups")
	public Response renderDefaultGroupsFragment(@PathParam("id") int orgId)
	{
		try
		{
			String html = webfragmentRenderer.renderDefaultGroupsFragment(orgId);
			return Response.ok(html).build();
			
		} catch (IOException e)
		{
			log.error("Failed to get default groups for organization with id " + orgId, e);
			return Response.serverError().build();

		}
	}
	
    @GET
    @AnonymousAllowed
    @Path("/integrated-accounts/reload")
	@Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_FORM_URLENCODED})
	@Produces({MediaType.TEXT_PLAIN})
    public Response reloadIntegratedAccountConfig()
    {
        try
        {
            ondemandAccountConfig.reload(true);
            return Response.ok("OK").build();
        } catch (Exception e)
        {
            log.error("Failed to reload config.", e);
            return Response.serverError().build();
        }
    }
	
}