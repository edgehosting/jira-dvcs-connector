package com.atlassian.jira.plugins.dvcs.model;

/**
 * Describes a user 
 */
public class DvcsUser
{
    public static final DvcsUser UNKNOWN_USER = new DvcsUser(
            "unknown", "Unknown User", "https://secure.gravatar.com/avatar/unknown?d=mm");

    private final String username;
    private final String fullName;
    private final String avatar;

    public DvcsUser(String username, String fullName, String avatar)
    {
        this.username = username;
        this.fullName = fullName;
        this.avatar = avatar;
    }

    public String getUsername()
    {
        return username;
    }

    public String getFullName()
    {
        return fullName;
    }

    public String getAvatar()
    {
        return avatar;
    }

}
