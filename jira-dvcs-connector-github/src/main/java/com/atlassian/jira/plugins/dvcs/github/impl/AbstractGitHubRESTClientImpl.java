package com.atlassian.jira.plugins.dvcs.github.impl;


import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.header.LinkHeader;
import com.sun.jersey.core.header.LinkHeaders;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.client.IGitHubConstants;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Resource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

/**
 * Support for {@link GitHubRESTClientImpl}.
 *
 * @author Stanislav Dvorscak
 */
public abstract class AbstractGitHubRESTClientImpl
{

    /**
     * Jersey client.
     */
    private final Client client;

    /**
     * @see #setRepositoryService(RepositoryService)
     */
    @Resource
    private RepositoryService repositoryService;

    /**
     * Constructor.
     */
    public AbstractGitHubRESTClientImpl()
    {
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        client = Client.create(clientConfig);
    }

    /**
     * @param repositoryService injected {@link RepositoryService} dependency
     */
    public void setRepositoryService(RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
    }

    /**
     * Corrects {@link Repository#getOrgHostUrl()} to point to correct repository API URL.
     *
     * @param repository for which repository
     * @return resolved REST API URL for provided repository
     */
    private String getRepositoryAPIUrl(Repository repository)
    {
        URL url;

        try
        {
            url = new URL(repository.getOrgHostUrl());
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }

        UriBuilder result;
        try
        {
            result = UriBuilder.fromUri(url.toURI());
        }
        catch (IllegalArgumentException e)
        {
            throw new RuntimeException(e);
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }

        String host = url.getHost();

        // corrects default GitHub URL and GIST url to default github host
        if (IGitHubConstants.HOST_DEFAULT.equals(host) || IGitHubConstants.HOST_GISTS.equals(host) || IGitHubConstants.HOST_API.equals(host))
        {
            result.host(IGitHubConstants.HOST_API);
        }
        else
        {
            result.path(IGitHubConstants.SEGMENT_V3_API);
        }

        result = result.path("/repos").path(repository.getOrgName()).path(repository.getSlug());

        // decorates URI with access token
        result.queryParam("access_token", "{arg1}");
        return result.build(repository.getCredential().getAccessToken()).toString();
    }

    /**
     * @return access to configured jersey client
     */
    protected Client getClient()
    {
        return client;
    }

    protected <T> List<T> getAll(WebResource webResource, Class<T[]> entityType)
    {
        webResource.header("Authorization", "Basic ");
        return getAll(webResource, entityType, null, null);
    }

    /**
     * Goes over all GitHub pages and return all pages union.
     *
     * @param webResource of first page
     * @param entityType type of entities
     * @return union
     */
    protected <T> List<T> getAll(WebResource webResource, Class<T[]> entityType, String username, String password)
    {
        List<T> result = new LinkedList<T>();

        WebResource cursor = webResource;
        do
        {
            WebResource.Builder builder = cursor.accept(MediaType.APPLICATION_JSON_TYPE);

            if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
            {
                String usernamePassword = username + ":" + password;
                usernamePassword = new String(Base64.encodeBase64(usernamePassword.getBytes()));
                builder = builder.header("Authorization", "Basic " + usernamePassword);
            }

            ClientResponse clientResponse = builder.get(ClientResponse.class);

            if (clientResponse.getStatus() < 300)
            {
                result.addAll(Arrays.asList(clientResponse.getEntity(entityType)));

                LinkHeaders linkHeaders = getLinks(clientResponse);
                LinkHeader nextLink = linkHeaders.getLink("next");
                URI nextPage = nextLink != null ? nextLink.getUri() : null;
                cursor = nextPage != null ? client.resource(nextPage) : null;
            }
            else
            {
                throw new UniformInterfaceException(clientResponse);
            }
        }
        while (cursor != null);
        return result;
    }

    /**
     * TODO: workaround for bug - {@link ClientResponse} of jersey does not support comma separated multiple values
     * headers
     *
     * @param clientResponse for processing
     * @return proceed links
     */
    private LinkHeaders getLinks(ClientResponse clientResponse)
    {
        // raw 'Link' headers values
        List<String> linksRaw = clientResponse.getHeaders().get("Link");
        if (linksRaw == null)
        {
            linksRaw = new LinkedList<String>();
        }

        // proceed 'Link' values according to multiple values header policy
        List<String> links = new LinkedList<String>();

        for (String linkRaw : linksRaw)
        {
            // header can be comma separated - which means, that it contains multiple values
            for (String link : linkRaw.split(","))
            {
                links.add(link.trim());
            }
        }

        MultivaluedMapImpl headers = new MultivaluedMapImpl();
        headers.put("Link", links);
        return new LinkHeaders(headers);
    }

    /**
     * Creates new {@link WebResource} without caching.
     *
     * @param repository over which reposiory
     * @param uri of resource
     * @return created web resource
     */
    protected WebResource resource(Repository repository, String uri)
    {
        WebResource result = client.resource(getRepositoryAPIUrl(repository)).path(uri);
        return result;
    }

}
