package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.resource.RemoteResource;
import com.atlassian.jira.plugins.bitbucket.bitbucket.resource.RootRemoteResource;
import com.atlassian.jira.plugins.bitbucket.bitbucket.resource.SubRemoteResource;
import com.atlassian.sal.api.net.RequestFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public class Bitbucket
{
    private static final String BASE_URL = "https://api.bitbucket.org/1.0/repositories/";

    private final RemoteResource remoteResource;

    public Bitbucket(final RequestFactory<?> requestFactory, String username, String password)
    {
        remoteResource = new RootRemoteResource(requestFactory, username, password, BASE_URL);
    }

    public Bitbucket(final RequestFactory<?> requestFactory)
    {
        remoteResource = new RootRemoteResource(requestFactory, BASE_URL);
    }

    /**
     * Retrieves information about a repository
     *
     * @param owner the owner of the project
     * @param slug  the slug of the project
     * @return the project
     */
    public BitbucketRepository getRepository(String owner, String slug)
    {
        try
        {
            String uri = URLEncoder.encode(owner, "UTF-8") + "/" + URLEncoder.encode(slug, "UTF-8");
            return BitbucketRepository.parse(new SubRemoteResource(remoteResource,uri), remoteResource.get(uri));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new BitbucketException("required encoding not found", e);
        }
    }

}
