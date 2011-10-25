package com.atlassian.jira.plugins.bitbucket.api.impl;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;

/**
 * Describes a user 
 */
public class DefaultSourceControlUser implements SourceControlUser
{
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String avatar;
    private final String resourceUri;

    public DefaultSourceControlUser(String username, String firstName, String lastName, String avatar, String resourceUri)
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
