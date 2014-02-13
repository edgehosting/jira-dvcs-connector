package com.atlassian.jira.plugins.dvcs.github.impl;

import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpStatus;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.github.api.GitHubRESTClient;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepositoryHook;
import com.atlassian.jira.plugins.dvcs.model.Repository;
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
            if (e.getResponse().getStatus() == HttpStatus.SC_UNPROCESSABLE_ENTITY)
            {
                throw new SourceControlException.PostCommitHookRegistrationException("Could not add request hook: "
                        + e.getResponse().getEntity(String.class), e);
            } else
            {
                // TODO: BBC-610 No i18n support in DVCS Connector
                throw new SourceControlException.PostCommitHookRegistrationException(
                        "Could not add request hook. Possibly due to lack of admin permissions.", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteHook(Repository repository, GitHubRepositoryHook hook)
    {
        WebResource webResource = newWebResource(repository, "/hooks/" + hook.getId());
        try
        {
            webResource.delete();
        } catch (UniformInterfaceException e)
        {
            throw new SourceControlException.PostCommitHookRegistrationException("Could not remove postcommit hook", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubRepositoryHook[] getHooks(Repository repository)
    {
        WebResource hooksWebResource = cachedWebResource(repository, "/hooks");
        return hooksWebResource.accept(MediaType.APPLICATION_JSON_TYPE).get(GitHubRepositoryHook[].class);
    }
}
