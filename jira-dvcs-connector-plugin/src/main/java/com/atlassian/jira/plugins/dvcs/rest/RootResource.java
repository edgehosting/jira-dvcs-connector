package com.atlassian.jira.plugins.dvcs.rest;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.RepositoryList;
import com.atlassian.jira.plugins.dvcs.model.RepositoryRegistration;
import com.atlassian.jira.plugins.dvcs.model.SentData;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfigService;
import com.atlassian.jira.plugins.dvcs.rest.common.Status;
import com.atlassian.jira.plugins.dvcs.rest.security.AdminOnly;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * The Class RootResource.
 */
@Path("/")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
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

    private final AccountsConfigService ondemandAccountConfig;

    /**
     * The Constructor.
     *
     * @param organizationService
     *            the organization service
     * @param repositoryService
     *            the repository service
     * @param ondemandAccountConfig
     *            OnDemand account config service
     */
    public RootResource(OrganizationService organizationService, RepositoryService repositoryService, AccountsConfigService ondemandAccountConfig)
    {
        this.organizationService = organizationService;
        this.repositoryService = repositoryService;
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
    @AdminOnly
    public Response getRepository(@PathParam("id") int id)
    {
        Repository repository = repositoryService.get(id);
        if (repository != null)
        {
            return Response.ok(repository).build();
        } else
        {
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
    @AdminOnly
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
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repository/{id}/sync")
    public Response startRepositorySync(@PathParam("id") int id, @FormParam("payload") String payload)
    {
        log.info("Postcommit hook started synchronization for repository [{}].", id);
        log.debug("Rest request to soft sync repository [{}] with payload [{}]", id, payload);

        try
        {
            repositoryService.sync(id,
                    EnumSet.of(SynchronizationFlag.SOFT_SYNC, SynchronizationFlag.SYNC_CHANGESETS, SynchronizationFlag.WEBHOOK_SYNC));

            return Response.ok().build();
        }
        catch (SourceControlException.SynchronizationDisabled e)
        {
            return buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    @AnonymousAllowed
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repository/{id}/sync")
    public Response startRepositoryPRSync(@PathParam("id") int id, String source)
    {
        log.info("Pull Request Postcommit hook started synchronization for repository [{}].", id);
        String key = null;

        try
        {
            JSONObject jsoned = new JSONObject(source);
            Iterator<String> keys = jsoned.keys();
            if (keys.hasNext())
            {
                key = keys.next();
            }
        }
        catch (JSONException e)
        {
            log.info("Could not parse json request.");
        }

        log.debug("Rest request to soft sync pull requests for repository [{}] with type [{}]", id, key);

        try
        {
            repositoryService.sync(id,
                EnumSet.of(SynchronizationFlag.SOFT_SYNC, SynchronizationFlag.SYNC_PULL_REQUESTS, SynchronizationFlag.WEBHOOK_SYNC));

            return Response.ok().build();
        }
        catch (SourceControlException.SynchronizationDisabled e)
        {
            return buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    /**
     * Start repository softsync.
     *
     * @param id
     *            the id
     * @return the response
     */
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repository/{id}/softsync")
    @AdminOnly
    public Response startRepositorySoftSync(@PathParam("id") int id)
    {
        log.debug("Rest request to softsync repository [{}] ", id);

        try
        {
            repositoryService.sync(id,
                    EnumSet.of(SynchronizationFlag.SOFT_SYNC, SynchronizationFlag.SYNC_CHANGESETS, SynchronizationFlag.SYNC_PULL_REQUESTS));
            // ...
            // redirect to Repository resource - that will contain sync
            // message/status
            UriBuilder ub = uriInfo.getBaseUriBuilder();
            URI uri = ub.path("/repository/{id}").build(id);

            return Response.seeOther(uri).build();
        }
        catch (SourceControlException.SynchronizationDisabled e)
        {
            return buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    /**
     * Start repository fullsync.
     *
     * @param id
     *            the id
     * @return the response
     */
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repository/{id}/fullsync")
    @AdminOnly
    public Response startRepositoryFullSync(@PathParam("id") int id)
    {
        log.debug("Rest request to fullsync repository [{}] ", id);

        try
        {
            repositoryService.sync(id, EnumSet.of(SynchronizationFlag.SYNC_CHANGESETS, SynchronizationFlag.SYNC_PULL_REQUESTS));

            // ...
            // redirect to Repository resource - that will contain sync
            // message/status
            UriBuilder ub = uriInfo.getBaseUriBuilder();
            URI uri = ub.path("/repository/{id}").build(id);

            return Response.seeOther(uri).build();
        }
        catch (SourceControlException.SynchronizationDisabled e)
        {
            return buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repository/{id}/fullSyncChangesets")
    @AdminOnly
    public Response startRepositoryChangesetsSynchronization(@PathParam("id") int id)
    {
        log.debug("Rest request to changesets fullsync repository [{}] ", id);

        try
        {
            repositoryService.sync(id, EnumSet.of(SynchronizationFlag.SYNC_CHANGESETS));

            // ...
            // redirect to Repository resource - that will contain sync
            // message/status
            UriBuilder ub = uriInfo.getBaseUriBuilder();
            URI uri = ub.path("/repository/{id}").build(id);

            return Response.seeOther(uri).build();
        }
        catch (SourceControlException.SynchronizationDisabled e)
        {
            return buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repository/{id}/fullSyncPullRequests")
    @AdminOnly
    public Response startRepositoryPullRequestsSynchronization(@PathParam("id") int id)
    {
        log.debug("Rest request to pull request fullsync repository [{}] ", id);

        try
        {
            repositoryService.sync(id, EnumSet.of(SynchronizationFlag.SYNC_PULL_REQUESTS));

            // ...
            // redirect to Repository resource - that will contain sync
            // message/status
            UriBuilder ub = uriInfo.getBaseUriBuilder();
            URI uri = ub.path("/repository/{id}").build(id);

            return Response.seeOther(uri).build();
        }
        catch (SourceControlException.SynchronizationDisabled e)
        {
            return buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, e.getMessage());
        }
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
    @AdminOnly
    public Response accountInfo(@QueryParam("server") String server, @QueryParam("account") String account)
    {
        if (StringUtils.isEmpty(server) || StringUtils.isEmpty(account))
        {
            log.debug("REST call /accountInfo contained empty server '{}' or account '{}' param", new Object[] { server, account });

            return Response.status(Response.Status.BAD_REQUEST).build();
        }

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
    @Path("/organization/{id}/tokenOwner")
    @AdminOnly
    public Response getTokenOwner(@PathParam("id") String organizationId)
    {
        if (organizationId == null)
        {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        DvcsUser currentUser;
        try
        {
            currentUser = organizationService.getTokenOwner(Integer.parseInt(organizationId));
            return Response.ok(currentUser).build();
        } catch (Exception e)
        {
            log.warn("Error retrieving token owner: " + e.getMessage(), e);
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/organization/{id}/syncRepoList")
    @AdminOnly
    public Response syncRepoList(@PathParam("id") String organizationId)
    {
        if (organizationId == null)
        {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Organization organization = organizationService.get(Integer.parseInt(organizationId), false);
        try
        {
            repositoryService.syncRepositoryList(organization);
        } catch (SourceControlException e)
        {
            log.error("Could not refresh repository list", e);
        }
        return Response.noContent().build();
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/org/{id}/autolink")
    @Consumes({ MediaType.APPLICATION_JSON })
    @AdminOnly
    public Response enableOrganizationAutolinkNewRepos(@PathParam("id") int id, SentData autolink)
    {
        organizationService.enableAutolinkNewRepos(id, Boolean.parseBoolean(autolink.getPayload()));
        return Response.noContent().build();
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/org/{id}/globalsmarts")
    @Consumes({ MediaType.APPLICATION_JSON })
    @AdminOnly
    public Response enableSmartcommitsOnNewRepos(@PathParam("id") int id, SentData autoinvite)
    {
        organizationService.enableSmartcommitsOnNewRepos(id, Boolean.parseBoolean(autoinvite.getPayload()));
        return Response.noContent().build();
    }

    @POST
    @Produces({ MediaType.APPLICATION_XML })
    @Path("/org/{id}/oauth")
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @AdminOnly
    public Response setOrganizationOAuth(@PathParam("id") int id, @FormParam("key") String key, @FormParam("secret") String secret)
    {
        Organization organization = organizationService.get(id, false);
        organizationService.updateCredentials(id, new Credential(key, secret, organization.getCredential().getAccessToken()));
        return Response.ok(organization).build();
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repo/{id}/autolink")
    @Consumes({ MediaType.APPLICATION_JSON })
    @AdminOnly
    public Response enableRepositoryAutolink(@PathParam("id") int id, SentData autolink)
    {
        RepositoryRegistration registration = repositoryService.enableRepository(id, Boolean.parseBoolean(autolink.getPayload()));
        return Response.ok(registration).build();
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repo/{id}/smart")
    @Consumes({ MediaType.APPLICATION_JSON })
    @AdminOnly
    public Response enableSmartcommits(@PathParam("id") int id, SentData enabled)
    {
        // todo handle exceptions
        repositoryService.enableRepositorySmartcommits(id, Boolean.parseBoolean(enabled.getPayload()));
        return Response.noContent().build();
    }

    @GET
    @Path("/organization/{id}/defaultgroups")
    @AdminOnly
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getDefaultGroups(@PathParam("id") int orgId)
    {
        Map<String, Object> result = new HashMap<String, Object>();

        Organization organization = organizationService.get(orgId, false);
        try
        {
            // organization
            Map<String, Object> organizationResult = new HashMap<String, Object>();
            result.put("organization", organizationResult);
            organizationResult.put("id", organization.getId());
            organizationResult.put("name", organization.getName());

            // groups
            List<Map<String, Object>> groupsResult = new LinkedList<Map<String, Object>>();
            result.put("groups", groupsResult);
            for (Group group : organizationService.getGroupsForOrganization(organization))
            {
                Map<String, Object> groupView = new HashMap<String, Object>();
                groupView.put("slug", group.getSlug());
                groupView.put("niceName", group.getNiceName());
                groupView.put("selected", organization.getDefaultGroups().contains(group));
                groupsResult.add(groupView);
            }

            return Response.ok(result).build();

        } catch (SourceControlException.Forbidden_403 e)
        {
            return buildErrorResponse(Response.Status.FORBIDDEN, "Unable to access Bitbucket");

        } catch (SourceControlException e)
        {
            return buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
                    "Error retrieving list of groups for " + organization.getOrganizationUrl()
                            + ". Please check JIRA logs for details.");
        }
    }

    @GET
    @Path("/defaultgroups")
    @AdminOnly
    public Response getDefaultGroups()
    {

        List<Map<String, Object>> organizations = new LinkedList<Map<String, Object>>();
        int groupsCount = 0;

        List<Map<String, Object>> errors = new LinkedList<Map<String, Object>>();

        for (Organization organization : organizationService.getAll(false, "bitbucket"))
        {
            try
            {
                Map<String, Object> organizationView = new HashMap<String, Object>();

                organizationView.put("id", organization.getId());
                organizationView.put("name", organization.getName());
                organizationView.put("organizationUrl", organization.getOrganizationUrl());

                List<Map<String, Object>> groups = new LinkedList<Map<String, Object>>();
                for (Group group : organizationService.getGroupsForOrganization(organization))
                {
                    groupsCount++;

                    Map<String, Object> groupView = new HashMap<String, Object>();
                    groupView.put("slug", group.getSlug());
                    groupView.put("niceName", group.getNiceName());
                    groupView.put("selected", organization.getDefaultGroups().contains(group));
                    groups.add(groupView);

                }

                organizationView.put("groups", groups);

                organizations.add(organizationView);

            } catch (Exception e)
            {
                log.warn("Failed to get groups for organization {}. Cause message is {}", organization.getName(), e.getMessage());

                Map<String, Object> groupView = new HashMap<String, Object>();
                groupView.put("organizationUrl", organization.getOrganizationUrl());
                groupView.put("name", organization.getName());
                errors.add(groupView);
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("organizations", organizations);
        result.put("groupsCount", groupsCount);
        result.put("errors", errors);

        return Response.ok(result).build();
    }

    @POST
    @Path("/linkers/{onoff}")
    @Consumes({ MediaType.TEXT_PLAIN })
    @Produces({ MediaType.TEXT_PLAIN })
    @AdminOnly
    public Response onOffLinkers(@PathParam("onoff") String onOff)
    {
        try
        {
            boolean onOffBoolean = BooleanUtils.toBoolean(onOff);
            repositoryService.onOffLinkers(onOffBoolean);
            return Response.ok("OK").build();
        } catch (Exception e)
        {
            log.error("Failed to reload config.", e);
            return Response.serverError().build();
        }
    }

    @GET
    @AnonymousAllowed
    @Path("/integrated-accounts/reload")
    @Consumes({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.TEXT_PLAIN })
    public Response reloadIntegratedAccountConfig()
    {
        try
        {
            ondemandAccountConfig.reloadAsync();
            return Response.ok("OK").build();
        } catch (Exception e)
        {
            log.error("Failed to reload config.", e);
            return Response.serverError().build();
        }
    }

    @GET
    @AnonymousAllowed
    @Path("/integrated-accounts/reloadSync")
    @Consumes({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.TEXT_PLAIN })
    public Response reloadSynchronousIntegratedAccountConfig()
    {
        try
        {
            ondemandAccountConfig.reload();
            return Response.ok("OK").build();
        } catch (Exception e)
        {
            log.error("Failed to reload config.", e);
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("/organization/{id}")
    @AdminOnly
    public Response deleteOrganization(@PathParam("id") int id)
    {
        Organization integratedAccount = organizationService.findIntegratedAccount();
        if (integratedAccount != null && id == integratedAccount.getId())
        {
            return buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to delete integrated account.");
        }

        if (organizationService.get(id, false) == null)
        {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try
        {
            organizationService.remove(id);
        } catch (Exception e)
        {
            log.error("Failed to remove account with id " + id, e);
            return buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to delete account.");
        }

        return Response.noContent().build();
    }

    private Response buildErrorResponse(Response.Status status, String message)
    {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setNoStore(true);

        Response.ResponseBuilder responseBuilder = Response.status(status).entity(new Status(status, message)).cacheControl(cacheControl);

        return responseBuilder.build();
    }
}