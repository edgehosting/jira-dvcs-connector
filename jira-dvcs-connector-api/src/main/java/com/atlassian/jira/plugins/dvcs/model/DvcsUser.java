package com.atlassian.jira.plugins.dvcs.model;

/**
 * Describes a user 
 */
public class DvcsUser
{
    public static final DvcsUser UNKNOWN_USER = new DvcsUser(
            "unknown", "", "", "https://secure.gravatar.com/avatar/unknown?d=mm");


    private final String username;
    private final String firstName;
    private final String lastName;
    private final String avatar;

    public DvcsUser(String username, String firstName, String lastName, String avatar)
    {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatar = avatar;
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

}
