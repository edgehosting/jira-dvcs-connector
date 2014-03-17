package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

public class BitbucketPullRequestParticipant
{
    private String role;
    private BitbucketUser user;
    private boolean approved;

    public String getRole()
    {
        return role;
    }

    public void setRole(final String role)
    {
        this.role = role;
    }

    public BitbucketUser getUser()
    {
        return user;
    }

    public void setUser(final BitbucketUser user)
    {
        this.user = user;
    }

    public boolean isApproved()
    {
        return approved;
    }

    public void setApproved(final boolean approved)
    {
        this.approved = approved;
    }
}
