package com.atlassian.jira.plugins.dvcs.github.impl;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.eclipse.egit.github.core.client.IGitHubConstants;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheFactory;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
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
     * Cache of created {@link WebResource}-s.
     */
    private Cache<WebResourceCacheKey, WebResource> webResourceCache;

    /**
     * @see #setCacheFactory(CacheFactory)
     */
    @Resource
    private CacheFactory cacheFactory;

    /**
     * @see #setRepositoryService(RepositoryService)
     */
    @Resource
    private RepositoryService repositoryService;

    /**
     * Key of {@link AbstractGitHubRESTClientImpl#webResourceCache}.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    public static final class WebResourceCacheKey implements Serializable
    {

        /**
         * Serial version id.
         */
        private static final long serialVersionUID = 1L;

        private final int repositoryId;
        private final String uri;

        private final int hashCode;

        public WebResourceCacheKey(Repository repository, String uri)
        {
            this.repositoryId = repository.getId();
            this.uri = uri;

            hashCode = new HashCodeBuilder().append(repositoryId).append(uri).toHashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof WebResourceCacheKey)
            {
                WebResourceCacheKey key = (WebResourceCacheKey) obj;
                EqualsBuilder equalsBuilder = new EqualsBuilder();
                equalsBuilder.append(repositoryId, key.repositoryId);
                equalsBuilder.append(uri, key.uri);
                return equalsBuilder.isEquals();
            } else
            {
                return false;
            }
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

    }

    /**
     * Cache value loader for {@link AbstractGitHubRESTClientImpl#webResourceCache}.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private final class WebResourceCacheLoader implements CacheLoader<WebResourceCacheKey, WebResource>
    {

        @Override
        public WebResource load(WebResourceCacheKey key)
        {
            Repository repository = repositoryService.get(key.repositoryId);
            WebResource result = client.resource(key.uri);
            result.addFilter(new AccessTokenFilter(repository.getCredential().getAccessToken()));
            return result;
        }
    }

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
            UriBuilder uriBuilder = UriBuilder.fromUri(uri);
            uriBuilder.queryParam("access_token", "{arg1}");
            cr.setURI(uriBuilder.build(accessToken));
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
     * Initializes this bean.
     */
    @PostConstruct
    public void init()
    {
        webResourceCache = cacheFactory.getCache(AbstractGitHubRESTClientImpl.class.getSimpleName() + ".webResourceCache",
                new WebResourceCacheLoader(), new CacheSettingsBuilder().local().expireAfterAccess(2, TimeUnit.HOURS).build());
    }

    /**
     * @param cacheFactory
     *            injected {@link CacheFactory} dependency.
     */
    public void setCacheFactory(CacheFactory cacheFactory)
    {
        this.cacheFactory = cacheFactory;
    }

    /**
     * @param repositoryService
     *            injected {@link RepositoryService} dependency
     */
    public void setRepositoryService(RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
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
        URL url;

        try
        {
            url = new URL(repository.getOrgHostUrl());
        } catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }

        UriBuilder result;
        try
        {
            result = UriBuilder.fromUri(url.toURI());
        } catch (IllegalArgumentException e)
        {
            throw new RuntimeException(e);
        } catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }

        String host = url.getHost();

        // corrects default GitHub URL and GIST url to default github host
        if (IGitHubConstants.HOST_DEFAULT.equals(host) || IGitHubConstants.HOST_GISTS.equals(host))
        {
            result.host(IGitHubConstants.HOST_API);
        }

        result.path("repos").path(repository.getOrgName()).path(repository.getSlug());

        return result.build().toString();
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
        uri = getRepositoryAPIUrl(repository) + uri;
        return webResourceCache.get(new WebResourceCacheKey(repository, uri));
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