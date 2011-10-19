package com.atlassian.jira.plugins.bitbucket.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.DELETE;
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

import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.DefaultProgress;
import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

@Path("/")
public class RootResource
{
    @Context UriInfo uriInfo;

    private final PermissionManager permissionManager;
    private final ProjectManager projectManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
	private final RepositoryManager globalRepositoryManager;
	private final Synchronizer synchronizer;

	private Function<SourceControlRepository, Repository> TO_REST_REPOSITORY = new Function<SourceControlRepository, Repository>()
	{
		public Repository apply(SourceControlRepository from)
		{
			Repository repo = new Repository(from.getId(), from.getProjectKey(), from.getUrl(), from.getUsername(), null); // don't include the password¶
			Iterator<DefaultProgress> progressIterator = synchronizer.getProgress(from).iterator();
			String syncStatusMessage = progressIterator.hasNext() ? progressIterator.next().render() : null;
			repo.setStatus(syncStatusMessage);
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
        return project != null &&
                permissionManager.hasPermission(Permissions.PROJECT_ADMIN, project, jiraAuthenticationContext.getLoggedInUser());
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
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
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
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
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/repositories/{projectKey}")
    public Response getRepositories(@PathParam("projectKey") String projectKey)
    {
        if (canAdmin(projectKey))
        {
            List<Repository> list = new ArrayList<Repository>();
        	List<SourceControlRepository> repositories = globalRepositoryManager.getRepositories(projectKey);
        	list.addAll(Lists.transform(repositories, TO_REST_REPOSITORY));

        	return Response.ok(new RepositoryList(list)).build();
        }
        else
            return Response.status(Response.Status.FORBIDDEN).build();
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/repositories/{id}/sync")
    public Response startRepositorySync(@PathParam("id") int id, @QueryParam("payload") String payload)
    {
    	SourceControlRepository repository = globalRepositoryManager.getRepository(id);
		List<Changeset> changesets = globalRepositoryManager.parsePayload(repository, payload);
		synchronizer.synchronize(repository, changesets);
		// redirect to Repository resource - that will contain sync message/status
		UriBuilder ub = uriInfo.getAbsolutePathBuilder();
		URI uri = ub.path("/repository/{id}").build(id);
		return Response.seeOther(uri).build();    	
    }

    @POST
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/repository")
    public Response addRepository(Repository repository)
    {
        if (canAdmin(repository.getProjectKey()))
        {
        	String url = repository.getUrl();
        	String projectKey = repository.getProjectKey();
        	String username = repository.getUsername();
        	String password = repository.getPassword();
        	
        	SourceControlRepository repo = globalRepositoryManager.addRepository(projectKey, url, username, password);
            return Response.ok(TO_REST_REPOSITORY.apply(repo)).build();
        }
        else
            return Response.status(Response.Status.FORBIDDEN).build();
    }

    @DELETE
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/repository/{id}")
    public Response removeRepository(@PathParam("id") int id)
    {
        SourceControlRepository repository = globalRepositoryManager.getRepository(id);
        if (canAdmin(repository.getProjectKey()))
        {
            globalRepositoryManager.removeRepository(id);
            return Response.ok().build();
        }
        else
            return Response.status(Response.Status.FORBIDDEN).build();
    }

}