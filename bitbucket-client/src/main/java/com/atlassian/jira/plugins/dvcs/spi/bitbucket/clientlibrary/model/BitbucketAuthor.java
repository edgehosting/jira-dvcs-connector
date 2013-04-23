package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

public class BitbucketAuthor
{
    private String raw;
    private BitbucketUser user; 

    public String getRaw()
    {
        return raw;
    }

    public void setRaw(String raw)
    {
        this.raw = raw;
    }

    public BitbucketUser getUser()
    {
        return user;
    }

    public void setUser(BitbucketUser user)
    {
        this.user = user;
    }
}
