package com.atlassian.jira.plugins.dvcs.github.impl;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.github.api.GitHubRESTClient;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepositoryHook;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.httpclient.HttpStatus;

import java.util.List;
import javax.ws.rs.core.MediaType;

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
        WebResource webResource = resource(repository, "/hooks");
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
        WebResource webResource = resource(repository, "/hooks/" + hook.getId());
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
    public List<GitHubRepositoryHook> getHooks(Repository repository)
    {
        WebResource hooksWebResource = resource(repository, "/hooks");
        return getAll(hooksWebResource, GitHubRepositoryHook[].class);
    }

}
