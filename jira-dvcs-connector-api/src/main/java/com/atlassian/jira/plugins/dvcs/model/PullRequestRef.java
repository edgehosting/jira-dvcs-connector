package com.atlassian.jira.plugins.dvcs.model;

/**
 * PullRequest reference
 *
 * @since v1.4.3
 */
public class PullRequestRef
{
    private String branch;
    private String repository;
    private String repository_url;

    public PullRequestRef(final String branch, final String repository, final String repository_url)
    {
        this.branch = branch;
        this.repository = repository;
        this.repository_url = repository_url;
    }

    public String getBranch()
    {
        return branch;
    }

    public void setBranch(final String branch)
    {
        this.branch = branch;
    }

    public String getRepository()
    {
        return repository;
    }

    public void setRepository(final String repository)
    {
        this.repository = repository;
    }
}
