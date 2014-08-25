package com.atlassian.jira.plugins.dvcs.github.impl;

import com.atlassian.jira.plugins.dvcs.github.api.GitHubRESTClient;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepositoryHook;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.ws.rs.core.MediaType;

/**
 * An implementation of {@link GitHubRESTClient}.
 *
 * @author Stanislav Dvorscak
 */
public class GitHubRESTClientImpl extends AbstractGitHubRESTClientImpl implements GitHubRESTClient
{

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubRepositoryHook addHook(Repository repository, GitHubRepositoryHook hook)
    {
        WebResource webResource = resource(repository, "/hooks");
        return webResource.type(MediaType.APPLICATION_JSON_TYPE).post(GitHubRepositoryHook.class, hook);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteHook(Repository repository, GitHubRepositoryHook hook)
    {
        WebResource webResource = resource(repository, "/hooks/" + hook.getId());
        webResource.delete();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubRepositoryHook> getHooks(Repository repository)
    {
        WebResource hooksWebResource = resource(repository, "/hooks");
        return getAll(hooksWebResource, GitHubRepositoryHook[].class);
    }

    @ParametersAreNonnullByDefault
    public List<GitHubRepositoryHook> getHooks(Repository repository, String username, String password)
    {
        WebResource hooksWebResource = resource(repository, "/hooks");

        final HTTPBasicAuthFilter httpBasicFilter = new HTTPBasicAuthFilter(username, password);
        return getAll(hooksWebResource, GitHubRepositoryHook[].class, httpBasicFilter);
    }
}
