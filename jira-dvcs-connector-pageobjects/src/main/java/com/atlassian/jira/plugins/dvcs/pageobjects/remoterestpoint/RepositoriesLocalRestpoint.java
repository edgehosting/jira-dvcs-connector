package com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.RepositoryList;
import com.atlassian.jira.plugins.dvcs.pageobjects.RestUrlBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

import javax.ws.rs.core.MediaType;

/**
 * {@link Repository} related resit point.
 *
 * @author Stanislav Dvorscak
 *
 */
public class RepositoriesLocalRestpoint
{

    /**
     * REST point for "/rest/bitbucket/1.0/repositories"
     *
     * @return {@link RepositoryList}
     */
    public RepositoryList getRepositories()
    {
        RestUrlBuilder url = new RestUrlBuilder("/rest/bitbucket/1.0/repositories");
        return getRepositories(url);
    }

    /**
     * REST point for "/rest/bitbucket/1.0/repositories"
     *
     * @param jira
     * @return {@link RepositoryList}
     */
    public RepositoryList getRepositories(JiraTestedProduct jira)
    {
        RestUrlBuilder url = new RestUrlBuilder(jira, "/rest/bitbucket/1.0/repositories");
        return getRepositories(url);
    }

    private RepositoryList getRepositories(RestUrlBuilder url)
    {
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(clientConfig);
        return client.resource(url.toString()).accept(MediaType.APPLICATION_JSON_TYPE).get(RepositoryList.class);
    }

    /**
     * REST point for "/rest/bitbucket/1.0/repository/{id}"
     *
     * @param repositoryId repository id
     * @return {@link Repository}
     */
    public Repository getRepository(int repositoryId)
    {
        RestUrlBuilder url = new RestUrlBuilder("/rest/bitbucket/1.0/repository/" + repositoryId);
        return getRepository(url);
    }

    /**
     * REST point for "/rest/bitbucket/1.0/repository/{id}"
     *
     * @param jira
     * @param repositoryId repository id
     * @return {@link Repository}
     */
    public Repository getRepository(JiraTestedProduct jira, int repositoryId)
    {
        RestUrlBuilder url = new RestUrlBuilder("/rest/bitbucket/1.0/repository/" + repositoryId);
        return getRepository(url);
    }

    private Repository getRepository(RestUrlBuilder url)
    {
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(clientConfig);
        return client.resource(url.toString()).accept(MediaType.APPLICATION_JSON_TYPE).get(Repository.class);
    }
}
