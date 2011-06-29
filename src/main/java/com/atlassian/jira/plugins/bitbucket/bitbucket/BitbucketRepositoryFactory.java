package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.DefaultBitbucketRepository;
import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.LazyLoadedBitbucketRepository;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Factory for {@link BitbucketRepository} implementations
 */
public class BitbucketRepositoryFactory
{

    /**
     * Load the remote repository details based on the authentication method, the repository owner and repository
     * slug
     * @param bitbucket the remote bitbucket service
     * @param auth the authentication method
     * @param owner the owner of the repository
     * @param slug the slug of the repository
     * @return the parsed {@link BitbucketRepository}
     */
    public static BitbucketRepository load(Bitbucket bitbucket, BitbucketAuthentication auth, String owner, String slug)
    {
        return new LazyLoadedBitbucketRepository(bitbucket, auth, owner, slug);
    }

    /**
     * Load the remote repository details based on the authentication method and remote url
     * @param bitbucket the remote bitbucket service
     * @param auth the authentication method
     * @param url the url of the repository
     * @return the parsed {@link BitbucketRepository}
     * @throws MalformedURLException if the url is not correctly formed
     */
    public static BitbucketRepository load(Bitbucket bitbucket, BitbucketAuthentication auth, String url) throws MalformedURLException
    {
        return load(bitbucket, auth, getOwner(url), getSlug(url));
    }

    /**
     * Extract the repository owner from the repository URL
     * @param url the repository URL
     * @return the owner of the repository
     * @throws MalformedURLException if the URL is not correctly formed
     */
    public static String getOwner(String url) throws MalformedURLException
    {
        return new URL(url).getPath().split("/")[1];
    }

    /**
     * Extract the repository slug from the repository URL
     * @param url the repository URL
     * @return the slug of the repository
     * @throws MalformedURLException if the URL is not correctly formed
     */
    public static String getSlug(String url) throws MalformedURLException
    {
        return new URL(url).getPath().split("/")[2];
    }

    /**
     * Parse the json object as a {@link BitbucketRepository}
     * @param json the json object describing the {@link BitbucketRepository}
     * @return the parsed {@link BitbucketRepository}
     */
    public static BitbucketRepository parse(JSONObject json)
    {
        try
        {
            return new DefaultBitbucketRepository(
                    json.getString("website"),
                    json.getString("name"),
                    json.getInt("followers_count"),
                    json.getString("owner"),
                    json.getString("logo"),
                    json.getString("resource_uri"),
                    json.getString("slug"),
                    json.getString("description")
            );
        }
        catch (JSONException e)
        {
            throw new BitbucketException("invalid json object", e);
        }
    }

    private BitbucketRepositoryFactory()
    {
    }

}
