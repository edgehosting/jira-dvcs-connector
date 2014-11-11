package com.atlassian.jira.plugins.dvcs.remoterestpoint;

import com.atlassian.jira.plugins.dvcs.github.impl.AbstractGitHubRESTClientImpl;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;

public class GitHubEventsChangeTracker extends AbstractGitHubRESTClientImpl
{
    public static final String IF_NONE_MATCH = "If-None-Match";
    private EntityTag entityTag;
    private Repository repository;
    private WebResource webResource;

    public GitHubEventsChangeTracker(String user, String password, String slug, String url)
    {
        getClient().addFilter(new HTTPBasicAuthFilter(user, password));
        repository = createRepository(user, slug, url);
        webResource = getEventResource();

        markState();
    }


    public void markState()
    {
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        entityTag = clientResponse.getEntityTag();
    }

    public boolean isModified()
    {
        ClientResponse clientResponse = webResource.accept(MediaType.APPLICATION_JSON_TYPE).header(IF_NONE_MATCH, entityTag.getValue()).get(ClientResponse.class);

        if  (clientResponse.getStatus() == ClientResponse.Status.NOT_MODIFIED.getStatusCode())
        {
            return false;
        }
        else if (clientResponse.getStatus() < 300)
        {
            entityTag = clientResponse.getEntityTag();
            return true;
        }
        else
        {
            throw new UniformInterfaceException(clientResponse);
        }
    }

    private WebResource getEventResource()
    {
        return resource(repository, "/events");
    }

    private Repository createRepository(String orgName, String slug, String url)
    {
        Repository repository = new Repository();
        repository.setOrgHostUrl(url);
        repository.setOrgName(orgName);
        repository.setSlug(slug);
        Credential credential = new Credential();
        credential.setAccessToken("");
        repository.setCredential(credential);
        return repository;
    }
}
