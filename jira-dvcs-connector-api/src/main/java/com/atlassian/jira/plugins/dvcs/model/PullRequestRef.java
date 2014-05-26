package com.atlassian.jira.plugins.dvcs.model;

import org.codehaus.jackson.annotate.JsonCreator;

/**
 * PullRequest reference
 *
 * @since v1.4.3
 */
public class PullRequestRef
{
    private String branch;
    private String repository;
    private String repositoryUrl;

    @JsonCreator
    private PullRequestRef() {}

    public PullRequestRef(final String branch, final String repository, final String repositoryUrl)
    {
        this.branch = branch;
        this.repository = repository;
        this.repositoryUrl = repositoryUrl;
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

    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }

    public void setRepositoryUrl(final String repositoryUrl)
    {
        this.repositoryUrl = repositoryUrl;
    }
}
