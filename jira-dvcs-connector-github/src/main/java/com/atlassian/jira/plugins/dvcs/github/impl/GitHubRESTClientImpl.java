package com.atlassian.jira.plugins.dvcs.github.impl;

import javax.ws.rs.core.MediaType;

import com.atlassian.jira.plugins.dvcs.github.api.GitHubRESTClient;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepositoryHook;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

/**
 * An implementation of {@link GitHubRESTClient}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubRESTClientImpl extends AbstractGitHubRESTClientImpl implements GitHubRESTClient
{

    /**
     * {@inheritDoc}
     * 
     * @return
     */
    @Override
    public GitHubRepositoryHook addHook(Repository repository, GitHubRepositoryHook hook)
    {
        WebResource webResource = cachedWebResource(repository, "/hooks");
        try
        {
            return webResource.type(MediaType.APPLICATION_JSON_TYPE).post(GitHubRepositoryHook.class, hook);
        } catch (UniformInterfaceException e)
        {
            throw new IllegalArgumentException("Could not add provided hook: " + e.getResponse().getEntity(String.class), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteHook(Repository repository, GitHubRepositoryHook hook)
    {
        WebResource webResource = newWebResource(repository, "/hooks/" + hook.getId());
        webResource.delete();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubRepositoryHook[] getHooks(Repository repository)
    {
        WebResource hooksWebResource = cachedWebResource(repository, "/hooks");
        return hooksWebResource.accept(MediaType.APPLICATION_JSON_TYPE).get(new GenericType<GitHubRepositoryHook[]>()
        {
        });
    }
}
