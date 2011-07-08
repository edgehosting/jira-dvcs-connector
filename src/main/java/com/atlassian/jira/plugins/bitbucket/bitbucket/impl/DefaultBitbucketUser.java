package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketException;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * Describes a user from bitbucket
 */
public class DefaultBitbucketUser implements BitbucketUser
{
    public static BitbucketUser UNKNOWN_USER = new DefaultBitbucketUser(
            "unknown", "", "", "https://secure.gravatar.com/avatar/unknown?d=mm", ""
    );

    public static DefaultBitbucketUser parse(JSONObject json)
    {
        try
        {
            return new DefaultBitbucketUser(
                    json.getJSONObject("user").getString("username"),
                    json.getJSONObject("user").getString("first_name"),
                    json.getJSONObject("user").getString("last_name"),
                    json.getJSONObject("user").getString("avatar"),
                    json.getJSONObject("user").getString("resource_uri")
            );
        }
        catch (JSONException e)
        {
            throw new BitbucketException("invalid json object", e);
        }
    }


    private final String username;
    private final String firstName;
    private final String lastName;
    private final String avatar;
    private final String resourceUri;

    public DefaultBitbucketUser(String username, String firstName, String lastName, String avatar, String resourceUri)
    {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatar = avatar;
        this.resourceUri = resourceUri;
    }

    public String getUsername()
    {
        return username;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public String getResourceUri()
    {
        return resourceUri;
    }
}
