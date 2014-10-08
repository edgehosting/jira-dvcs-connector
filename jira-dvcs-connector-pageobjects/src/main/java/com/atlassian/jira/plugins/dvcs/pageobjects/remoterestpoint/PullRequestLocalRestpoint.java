package com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.atlassian.jira.plugins.dvcs.pageobjects.RestUrlBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

import javax.ws.rs.core.MediaType;

/**
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
public class PullRequestLocalRestpoint
{

    /**
     * Hack for generic de-serialization.
     *
     * @author Stanislav Dvorscak
     *
     */
    public static class RestDevResponseForPrRepository extends RestDevResponse<RestPrRepository>
    {
    }

    /**
     * REST point for "/rest/bitbucket/1.0/jira-dev/pr-detail?issue=" + issueKey
     *
     * @param issueKey
     * @return RestDevResponse<RestPrRepository>
     */
    public RestDevResponse<RestPrRepository> getPullRequest(String issueKey)
    {
        RestUrlBuilder url = new RestUrlBuilder("/rest/bitbucket/1.0/jira-dev/pr-detail?issue=" + issueKey);
        return getPullRequest(url);
    }

    /**
     * REST point for "/rest/bitbucket/1.0/jira-dev/pr-detail?issue=" + issueKey
     *
     * @param issueKey
     * @param jira
     * @return RestDevResponse<RestPrRepository>
     */
    public RestDevResponse<RestPrRepository> getPullRequest(String issueKey, JiraTestedProduct jira)
    {
        RestUrlBuilder url = new RestUrlBuilder(jira, "/rest/bitbucket/1.0/jira-dev/pr-detail?issue=" + issueKey);
        return getPullRequest(url);
    }

    private RestDevResponse<RestPrRepository> getPullRequest(RestUrlBuilder url)
    {
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(clientConfig);
        return client.resource(url.toString()).accept(MediaType.APPLICATION_JSON_TYPE).get(RestDevResponseForPrRepository.class);
    }
}
