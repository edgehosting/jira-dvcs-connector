package com.atlassian.jira.plugins.dvcs.model;

public class Reviewer
{
    private boolean approved;
    private String username;

    public boolean isApproved()
    {
        return approved;
    }

    public void setApproved(final boolean approved)
    {
        this.approved = approved;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(final String username)
    {
        this.username = username;
    }
}
