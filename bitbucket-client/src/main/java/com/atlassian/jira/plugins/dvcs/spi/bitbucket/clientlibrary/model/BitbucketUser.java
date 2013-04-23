package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

public class BitbucketUser
{
    private String username;
    private String displayName;
    
    public String getUsername()
    {
        return username;
    }
    public void setUsername(String username)
    {
        this.username = username;
    }
    public String getDisplayName()
    {
        return displayName;
    }
    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }
}
