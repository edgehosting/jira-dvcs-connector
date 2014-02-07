package com.atlassian.jira.plugins.dvcs.github.impl;

import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_GISTS;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.json.JSONConfiguration;

/**
 * Support for {@link GitHubRESTClientImpl}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class AbstractGitHubRESTClientImpl
{

    /**
     * Jersey client.
     */
    private final Client client;

    /**
     * {@link WebResource}-s cache of created instances.
     * 
     * @see #cachedWebResource(Repository, String)
     */
    private final ConcurrentMap<Integer, ConcurrentMap<String, WebResource>> webResourceByRepositoryAndUri = new ConcurrentHashMap<Integer, ConcurrentMap<String, WebResource>>();

    /**
     * Adds access token to each request.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private class AccessTokenFilter extends ClientFilter
    {

        /**
         * AccessToken, which will be used for requests decoration.
         */
        private String accessToken;

        /**
         * Constructor.
         * 
         * @param accessToken
         *            which will be used for requests decoration
         */
        private AccessTokenFilter(String accessToken)
        {
            this.accessToken = accessToken;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ClientResponse handle(ClientRequest cr) throws ClientHandlerException
        {
            URI uri = cr.getURI();
            StringBuilder query = new StringBuilder(uri.getQuery() != null ? uri.getQuery() : "");
            if (!StringUtils.isBlank(uri.getQuery()))
            {
                query.append('&');
            }
            query.append("access_token=").append(accessToken);

            try
            {
                cr.setURI(new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), query.toString(), uri
                        .getFragment()));
            } catch (URISyntaxException e)
            {
                throw new ClientHandlerException(e);
            }

            return getNext().handle(cr);
        }
    }

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
     * Corrects {@link Repository#getOrgHostUrl()} to point to correct repository API URL.
     * 
     * @param repository
     *            for which repository
     * @return resolved REST API URL for provided repository
     */
    private String getRepositoryAPIUrl(Repository repository)
    {
        URL urlObject;
        try
        {
            urlObject = new URL(repository.getOrgHostUrl());
        } catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }

        String host = urlObject.getHost();

        StringBuilder result = new StringBuilder();

        // corrects default GitHub URL and GIST url to default github host
        if ("github.com".equals(host) || HOST_GISTS.equals("gist.github.com"))
        {
            result.append("https://api.github.com");
        } else
        {
            result.append(urlObject.getProtocol()).append("://").append(urlObject.getHost());
        }

        result.append("/repos/").append(repository.getOwner()).append('/').append(repository.getSlug());

        return result.toString();
    }

    /**
     * @param repository
     *            for which repository
     * @param uri
     *            of web resource (without repository url)
     * @return cached web resource (if does not exist it will be created and inserted into the cache)
     */
    protected WebResource cachedWebResource(Repository repository, String uri)
    {
        // adds repository url to uri
        uri = getRepositoryAPIUrl(repository) + uri;

        ConcurrentMap<String, WebResource> webResourceByUri = webResourceByRepositoryAndUri.get(repository.getId());
        if (webResourceByUri == null)
        {
            webResourceByRepositoryAndUri.putIfAbsent(repository.getId(), new ConcurrentHashMap<String, WebResource>());
            webResourceByUri = webResourceByRepositoryAndUri.get(repository.getId());
        }

        WebResource result = webResourceByUri.get(uri);
        if (result == null)
        {
            synchronized (webResourceByUri)
            {
                result = webResourceByUri.get(uri);
                if (result == null)
                {
                    webResourceByUri.put(uri, result = client.resource(uri));
                }
            }
        }

        result.addFilter(new AccessTokenFilter(repository.getCredential().getAccessToken()));

        return result;
    }

    /**
     * Creates new {@link WebResource} without caching.
     * 
     * @param repository
     *            over which reposiory
     * @param uri
     *            of resource
     * @return created web resource
     */
    protected WebResource newWebResource(Repository repository, String uri)
    {
        uri = getRepositoryAPIUrl(repository) + uri;
        WebResource result = client.resource(uri);
        result.addFilter(new AccessTokenFilter(repository.getCredential().getAccessToken()));
        return result;
    }

}