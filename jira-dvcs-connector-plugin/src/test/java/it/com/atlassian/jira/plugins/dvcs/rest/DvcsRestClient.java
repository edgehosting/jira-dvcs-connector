package it.com.atlassian.jira.plugins.dvcs.rest;

import com.atlassian.jira.testkit.client.JIRAEnvironmentData;
import com.atlassian.jira.testkit.client.RestApiClient;
import com.sun.jersey.api.client.WebResource;

import java.net.URL;

import static com.google.common.base.Preconditions.checkNotNull;

public class DvcsRestClient<T extends DvcsRestClient<T>> extends RestApiClient<T>
{
    private final URL baseURL;

    protected DvcsRestClient(JIRAEnvironmentData environmentData)
    {
        super(checkNotNull(environmentData));
        baseURL = environmentData.getBaseUrl();
    }

    @Override
    protected WebResource createResource()
    {
        return resourceRoot(baseURL.toString()).path("rest/bitbucket/1.0");
    }

    @Override
    protected WebResource resourceRoot(final String url)
    {
        return super.resourceRoot(url);
    }
}
