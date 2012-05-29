package com.atlassian.jira.plugins.dvcs.rest;

import java.net.URI;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.RepositoryList;
import com.atlassian.jira.plugins.dvcs.model.SentData;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
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

	/**
	 * The Constructor.
	 * 
	 * @param organizationService
	 *            the organization service
	 * @param repositoryService
	 *            the repository service
	 */
	public RootResource(OrganizationService organizationService, RepositoryService repositoryService)
	{
		this.organizationService = organizationService;
		this.repositoryService = repositoryService;
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

		return Response.ok(repository).build();
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

		repositoryService.sync(id, false);

		// ...
		// redirect to Repository resource - that will contain sync
		// message/status
		UriBuilder ub = uriInfo.getBaseUriBuilder();
		URI uri = ub.path("/repository/{id}").build(id);

		return Response.seeOther(uri).build();
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
	@Path("/org/{id}/autoinvite")
	@Consumes({MediaType.APPLICATION_JSON})
	public Response enableOrganizationAutoInviteUsers(@PathParam("id") int id,
			SentData autoinvite)
	{

		organizationService.enableAutoInviteUsers(id, Boolean.parseBoolean(autoinvite.getPayload()));
		return Response.noContent().build();
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Path("/repo/{id}/autolink")
	@Consumes({MediaType.APPLICATION_JSON})
	public Response enableRepositoryAutolink(@PathParam("id") int id, SentData autolink)
	{
		// todo handle exceptions
		repositoryService.enableRepository(id, Boolean.parseBoolean(autolink.getPayload()));
		return Response.noContent().build();
	}
	
}