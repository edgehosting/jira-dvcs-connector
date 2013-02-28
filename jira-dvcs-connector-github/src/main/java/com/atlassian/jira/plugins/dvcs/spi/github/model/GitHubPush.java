package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains information, which happened when push was realized.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPush extends GitHubEntity
{

    /**
     * @see #getCreatedAt()
     */
    private Date createdAt;

    /**
     * @see #getCreatedBy()
     */
    private GitHubUser createdBy;

    /**
     * @see #getRepository()
     */
    private GitHubRepository repository;

    /**
     * @see #getRef()
     */
    private String ref;

    /**
     * @see #getBefore()
     */
    private String before;

    /**
     * @see #getHead()
     */
    private String head;

    /**
     * @see #getCommits()
     */
    private List<GitHubCommit> commits = new LinkedList<GitHubCommit>();

    /**
     * Constructor.
     */
    public GitHubPush()
    {
    }

    /**
     * @return {@link GitHubPush}
     */
    public Date getCreatedAt()
    {
        return createdAt;
    }

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    /**
     * @return Creator of the push.
     */
    public GitHubUser getCreatedBy()
    {
        return createdBy;
    }

    /**
     * @param createdBy
     *            {@link #getCreatedBy()}
     */
    public void setCreatedBy(GitHubUser createdBy)
    {
        this.createdBy = createdBy;
    }

    /**
     * @return Owner of the push.
     */
    public GitHubRepository getRepository()
    {
        return repository;
    }

    /**
     * @param repository
     *            {@link #getRepository()}
     */
    public void setRepository(GitHubRepository repository)
    {
        this.repository = repository;
    }

    /**
     * @return Ref of the push.
     */
    public String getRef()
    {
        return ref;
    }

    /**
     * @param ref
     *            {@link #getRef()}
     */
    public void setRef(String ref)
    {
        this.ref = ref;
    }

    /**
     * @return {@link GitHubCommit#getSha()} of the previous head.
     */
    public String getBefore()
    {
        return before;
    }

    /**
     * @param before
     *            {@link #getBefore()}
     */
    public void setBefore(String before)
    {
        this.before = before;
    }

    /**
     * @return new head
     */
    public String getHead()
    {
        return head;
    }

    /**
     * @param head
     *            {@link #getHead()}
     */
    public void setHead(String head)
    {
        this.head = head;
    }

    /**
     * @return Commits related to this push.
     */
    public List<GitHubCommit> getCommits()
    {
        return commits;
    }

    /**
     * @param commits
     *            {@link #getCommits()}
     */
    public void setCommits(List<GitHubCommit> commits)
    {
        this.commits = commits;
    }

}
