package com.atlassian.jira.plugins.dvcs.model;

/**
 * Describes a user 
 */
public class DvcsUser
{
//    public static final DvcsUser UNKNOWN_USER = new DvcsUser(
//            "unknown", "Unknown User", "https://secure.gravatar.com/avatar/unknown?d=mm");
    
    public static class UnknownUser extends DvcsUser
    {
        public UnknownUser(String username, String hostUrl)
        {
            super(username, "Unknown User", "https://secure.gravatar.com/avatar/unknown?d=mm", hostUrl);
        }
    }

    private final String username;
    private final String fullName;
    private final String avatar;
    private final String url;

    public DvcsUser(String username, String fullName, String avatar, String hostUrl)
    {
        this.username = username;
        this.fullName = fullName;
        this.avatar = avatar;
        this.url = hostUrl +"/"+ username;
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
    
    public String getUrl()
    {
        return url;
    }
}
