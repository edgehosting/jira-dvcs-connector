package com.atlassian.jira.plugins.dvcs.model;

/**
 * PullRequest reference
 *
 * @since v1.4.3
 */
public class PullRequestRef
{
    private String branch;
    private Repository repository;

    public PullRequestRef(final String branch, final Repository repository)
    {
        this.branch = branch;
        this.repository = repository;
    }

    public String getBranch()
    {
        return branch;
    }

    public void setBranch(final String branch)
    {
        this.branch = branch;
    }

    public Repository getRepository()
    {
        return repository;
    }

    public void setRepository(final Repository repository)
    {
        this.repository = repository;
    }
}
