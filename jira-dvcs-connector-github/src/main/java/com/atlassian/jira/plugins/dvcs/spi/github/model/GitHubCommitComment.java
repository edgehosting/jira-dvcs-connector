package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;

/**
 * Comment assigned to the {@link GitHubCommit}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommitComment extends GitHubEntity
{

    /**
     * @see #getGitHubId()
     */
    private long gitHubId;

    /**
     * @see #getCreatedAt()
     */
    private Date createdAt;

    /**
     * @see #getCreatedBy()
     */
    private GitHubUser createdBy;

    /**
     * @see #getCommit()
     */
    private GitHubCommit commit;

    /**
     * @see #getUrl()
     */
    private String url;

    /**
     * @see #getHtmlUrl()
     */
    private String htmlUrl;

    /**
     * @see #getText()
     */
    private String text;

    /**
     * Constructor.
     */
    public GitHubCommitComment()
    {
    }

    /**
     * @return GitHub identity.
     */
    public long getGitHubId()
    {
        return gitHubId;
    }

    /**
     * @param gitHubId
     *            {@link #getGitHubId()}
     */
    public void setGitHubId(long gitHubId)
    {
        this.gitHubId = gitHubId;
    }

    /**
     * @return Date of creation of this comment.
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
     * @return User, whose creates this comment.
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
     * @return For which commit is this comment.
     */
    public GitHubCommit getCommit()
    {
        return commit;
    }

    /**
     * @param commit
     *            {@link #getGitHubCommit()}
     */
    public void setCommit(GitHubCommit commit)
    {
        this.commit = commit;
    }

    /**
     * @return URL of the comment.
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @param url
     *            {@link #getUrl()}
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * @return HTML version of the {@link #getUrl()}
     */
    public String getHtmlUrl()
    {
        return htmlUrl;
    }

    /**
     * @param htmlUrl
     *            {@link #getHtmlUrl()}
     */
    public void setHtmlUrl(String htmlUrl)
    {
        this.htmlUrl = htmlUrl;
    }

    /**
     * @return The text/comment assigned to the {@link #getGitHubCommit()}.
     */
    public String getText()
    {
        return text;
    }

    /**
     * @param text
     *            {@link #getText()}
     */
    public void setText(String text)
    {
        this.text = text;
    }

}
