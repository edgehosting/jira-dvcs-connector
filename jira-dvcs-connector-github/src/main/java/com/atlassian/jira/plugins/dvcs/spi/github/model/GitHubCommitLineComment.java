package com.atlassian.jira.plugins.dvcs.spi.github.model;

import java.util.Date;

/**
 * Line commit comment.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommitLineComment extends GitHubEntity
{

    /**
     * @see #getGitHubId()
     */
    private long gitHubId;

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
     * @see #getCreatedAt()
     */
    private Date createdAt;

    /**
     * @see #getCreatedBy()
     */
    private GitHubUser createdBy;

    /**
     * @see #getPath()
     */
    private String path;

    /**
     * @see #getLine()
     */
    private int line;

    /**
     * @see #getText()
     */
    private String text;

    /**
     * Constructor.
     */
    public GitHubCommitLineComment()
    {
    }

    /**
     * @return GitHub identity of this entity.
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
     * @return Commit of the file.
     */
    public GitHubCommit getCommit()
    {
        return commit;
    }

    /**
     * @param commit
     *            {@link #getCommit()}
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
     * @return Date of creation.
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
     * @return Creator of comment.
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
     * @return The file path of the commented file.
     */
    public String getPath()
    {
        return path;
    }

    /**
     * @param path
     *            {@link #getPath()}
     */
    public void setPath(String path)
    {
        this.path = path;
    }

    /**
     * @return Line number of the commit.
     */
    public int getLine()
    {
        return line;
    }

    /**
     * @param line
     *            {@link #getLine()}
     */
    public void setLine(int line)
    {
        this.line = line;
    }

    /**
     * @return Text of the comment.
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
