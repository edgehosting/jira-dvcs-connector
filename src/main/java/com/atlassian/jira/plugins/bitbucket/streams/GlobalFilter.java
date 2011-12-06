package com.atlassian.jira.plugins.bitbucket.streams;

/**
 *
 */
public class GlobalFilter
{
    private Iterable<String> inProjects;
    private Iterable<String> notInProjects;
    private Iterable<String> inUsers;
    private Iterable<String> notInUsers;
    private Iterable<String> inIssues;
    private Iterable<String> notInIssues;

    public Iterable<String> getInProjects()
    {
        return inProjects;
    }

    public void setInProjects(Iterable<String> inProjects)
    {
        this.inProjects = inProjects;
    }

    public Iterable<String> getNotInProjects()
    {
        return notInProjects;
    }

    public void setNotInProjects(Iterable<String> notInProjects)
    {
        this.notInProjects = notInProjects;
    }

    public Iterable<String> getInUsers()
    {
        return inUsers;
    }

    public void setInUsers(Iterable<String> inUsers)
    {
        this.inUsers = inUsers;
    }

    public Iterable<String> getNotInUsers()
    {
        return notInUsers;
    }

    public void setNotInUsers(Iterable<String> notInUsers)
    {
        this.notInUsers = notInUsers;
    }

    public Iterable<String> getInIssues()
    {
        return inIssues;
    }

    public void setInIssues(Iterable<String> inIssues)
    {
        this.inIssues = inIssues;
    }

    public Iterable<String> getNotInIssues()
    {
        return notInIssues;
    }

    public void setNotInIssues(Iterable<String> notInIssues)
    {
        this.notInIssues = notInIssues;
    }
}
