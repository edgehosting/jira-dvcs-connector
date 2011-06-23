package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.DefaultBitbucketRepository;
import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.LazyLoadedBitbucketRepository;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * Factory for {@link BitbucketRepository} implementations
 */
public class BitbucketRepositoryFactory
{
    public static BitbucketRepository load(Bitbucket bitbucket, BitbucketAuthentication auth, String owner, String slug)
    {
        return new LazyLoadedBitbucketRepository(bitbucket, auth, owner, slug);
    }

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
