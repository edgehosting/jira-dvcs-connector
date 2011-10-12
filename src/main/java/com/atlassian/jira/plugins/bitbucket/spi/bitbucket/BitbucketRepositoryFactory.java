package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import java.net.MalformedURLException;
import java.net.URL;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.DefaultBitbucketRepository;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * Factory for {@link DefaultSourceControlRepository} implementations
 */
public class BitbucketRepositoryFactory
{

    /**
     * Extract the repository owner from the repository URL
     *
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
     *
     * @param url the repository URL
     * @return the slug of the repository
     * @throws MalformedURLException if the URL is not correctly formed
     */
    public static String getSlug(String url) throws MalformedURLException
    {
        return new URL(url).getPath().split("/")[2];
    }

    /**
     * Parse the json object as a {@link DefaultSourceControlRepository}
     *
     * @param json the json object describing the {@link DefaultSourceControlRepository}
     * @return the parsed {@link DefaultSourceControlRepository}
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
            throw new SourceControlException("invalid json object", e);
        }
    }

    private BitbucketRepositoryFactory()
    {
    }

}
