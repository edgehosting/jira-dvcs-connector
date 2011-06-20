package com.atlassian.jira.plugins.bitbucket.bitbucket.remote;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketException;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketRepository;
import com.atlassian.jira.plugins.bitbucket.bitbucket.connection.BitbucketConnection;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * Describes a repository on bitbucket
 */
public class RemoteBitbucketRepository implements BitbucketRepository
{

    public static RemoteBitbucketRepository parse(BitbucketConnection connection, JSONObject json)
    {
        try
        {
            return new RemoteBitbucketRepository(
                    connection,
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

    private final BitbucketConnection bitbucketConnection;
    private final String website;
    private final String name;
    private final int followers;
    private final String owner;
    private final String logo;
    private final String resourceUri;
    private final String slug;
    private final String description;



    public RemoteBitbucketRepository(BitbucketConnection bitbucketConnection,
                                     String website, String name, int followers, String owner,
                                     String logo, String resourceUri, String slug, String description)
    {
        this.bitbucketConnection = bitbucketConnection;
        this.website = website;
        this.name = name;
        this.followers = followers;
        this.owner = owner;
        this.logo = logo;
        this.resourceUri = resourceUri;
        this.slug = slug;
        this.description = description;
    }

    public String getWebsite()
    {
        return website;
    }

    public String getName()
    {
        return name;
    }

    public int getFollowers()
    {
        return followers;
    }

    public String getOwner()
    {
        return owner;
    }

    public String getLogo()
    {
        return logo;
    }

    public String getResourceUri()
    {
        return resourceUri;
    }

    public String getSlug()
    {
        return slug;
    }

    public String getDescription()
    {
        return description;
    }
}
