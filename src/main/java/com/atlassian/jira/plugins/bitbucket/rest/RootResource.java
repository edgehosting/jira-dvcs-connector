package com.atlassian.jira.plugins.bitbucket.rest;

import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Progress;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.UrlInfo;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.theplugin.commons.util.DateUtil;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Path("/")
public class RootResource
{

    @Context
    UriInfo uriInfo;

    private final Logger log = LoggerFactory.getLogger(RootResource.class);

    private final PermissionManager permissionManager;
    private final ProjectManager projectManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final RepositoryManager globalRepositoryManager;
    private final Synchronizer synchronizer;
    
    private final Function<SourceControlRepository, Repository> TO_REST_REPOSITORY = new Function<SourceControlRepository, Repository>()
    {
        @Override
        public Repository apply(SourceControlRepository from)
        {

            final String relativePastDate = DateUtil.getRelativePastDate(new Date(), globalRepositoryManager.getLastCommitDate(from));

            Repository repo = new Repository(from.getId(), from.getRepositoryType(), from.getProjectKey(), from.getRepositoryUri().getRepositoryUrl(),
                    from.getUsername(), null, from.getAdminUsername(), null, null, relativePastDate); // don't include password or accessToken
            Progress progress = synchronizer.getProgress(from);
            if (progress != null)
                repo.setStatus(new SyncProgress(progress.isFinished(), progress.getChangesetCount(), progress
                        .getJiraCount(), progress.getSynchroErrorCount(),progress.getError()));
            return repo;
        }
    };

    public RootResource(@Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager,
            PermissionManager permissionManager, ProjectManager projectManager,
            JiraAuthenticationContext jiraAuthenticationContext, Synchronizer synchronizer)
    {
        this.globalRepositoryManager = globalRepositoryManager;
        this.permissionManager = permissionManager;
        this.projectManager = projectManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.synchronizer = synchronizer;
    }

    private boolean canAdmin(String projectKey)
    {
        Project project = projectManager.getProjectObjByKey(projectKey);
        return project != null
                && (permissionManager.hasPermission(Permissions.PROJECT_ADMIN, project,
                        jiraAuthenticationContext.getLoggedInUser()) || permissionManager.hasPermission(
                        Permissions.ADMINISTER, jiraAuthenticationContext.getLoggedInUser()));
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repositories/")
    public Response getAllRepositories()
    {
        List<Repository> list = Lists.newArrayList();
        List<Project> projects = projectManager.getProjectObjects();
        for (Project project : projects)
        {
            if (canAdmin(project.getKey()))
            {
                List<SourceControlRepository> repositories = globalRepositoryManager.getRepositories(project.getKey());
                list.addAll(Lists.transform(repositories, TO_REST_REPOSITORY));
            }
        }
        return Response.ok(new RepositoryList(list)).build();
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repository/{id}")
    public Response getRepository(@PathParam("id") int id)
    {
        SourceControlRepository repository = globalRepositoryManager.getRepository(id);
        if (repository != null && canAdmin(repository.getProjectKey()))
            return Response.ok(TO_REST_REPOSITORY.apply(repository)).build();
        else
            return Response.status(Response.Status.FORBIDDEN).build();
    }
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repositories/{projectKey}")
    public Response getRepositories(@PathParam("projectKey") String projectKey)
    {
        if (canAdmin(projectKey))
        {
            List<Repository> list = new ArrayList<Repository>();
            List<SourceControlRepository> repositories = globalRepositoryManager.getRepositories(projectKey);
            list.addAll(Lists.transform(repositories, TO_REST_REPOSITORY));

            return Response.ok(new RepositoryList(list)).build();
        } else
            return Response.status(Response.Status.FORBIDDEN).build();
    }

    @AnonymousAllowed
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repository/{id}/sync")
    public Response startRepositorySync(@PathParam("id") int id, @FormParam("payload") String payload)
    {
        log.debug("Rest request to sync repository [{}] with payload [{}]", id, payload);
        SourceControlRepository repository = globalRepositoryManager.getRepository(id);
        if (payload == null)
        {
            synchronizer.synchronize(repository);
        } else
        {
            List<Changeset> changesets = globalRepositoryManager.parsePayload(repository, payload);
            synchronizer.synchronize(repository, changesets);
        }
        // redirect to Repository resource - that will contain sync message/status
        UriBuilder ub = uriInfo.getBaseUriBuilder();
        URI uri = ub.path("/repository/{id}").build(id);
        return Response.seeOther(uri).build();
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repository")
    public Response addRepository(Repository repository)
    {
        if (canAdmin(repository.getProjectKey()))
        {
            String url = repository.getUrl();
            String repositoryType = repository.getRepositoryType();
            String projectKey = repository.getProjectKey();
            String username = repository.getUsername();
            String password = repository.getPassword();
            String adminUsername = repository.getUsername();
            String adminPassword = repository.getPassword();
            String accessToken = repository.getAccessToken();

            SourceControlRepository repo;
            try
            {
                repo = globalRepositoryManager.addRepository(repositoryType, projectKey, url, username, password,
                        adminUsername, adminPassword, accessToken);
            } catch (SourceControlException e)
            {
                return Response.serverError().entity(e).build();
            }
            return Response.ok(TO_REST_REPOSITORY.apply(repo)).build();
        } else
            return Response.status(Response.Status.FORBIDDEN).build();
    }

    @DELETE
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/repository/{id}")
    public Response removeRepository(@PathParam("id") int id)
    {
        SourceControlRepository repository = globalRepositoryManager.getRepository(id);
        if (canAdmin(repository.getProjectKey()))
        {
            SourceControlRepository repo = globalRepositoryManager.getRepository(id);
            globalRepositoryManager.removeRepository(id);
            globalRepositoryManager.removePostcommitHook(repo);
            return Response.ok().build();
        } else
            return Response.status(Response.Status.FORBIDDEN).build();
    }

    
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Path("/urlinfo")
    public Response urlInfo(@QueryParam("repositoryUrl") String repositoryUrl, @QueryParam("projectKey") String projectKey)
    {
        UrlInfo urlInfo = globalRepositoryManager.getUrlInfo(repositoryUrl.trim(), projectKey);
        if (urlInfo!=null)
            return Response.ok(urlInfo).build();
        else 
            return Response.status(Response.Status.NOT_FOUND).build();
    }
}