package com.atlassian.jira.plugins.dvcs.remoterestpoint;

import javax.ws.rs.core.MediaType;

import com.atlassian.jira.plugins.dvcs.RestUrlBuilder;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.RepositoryList;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

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
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(clientConfig);
        return client.resource(url.toString()).accept(MediaType.APPLICATION_JSON_TYPE).get(RepositoryList.class);
    }

}