package com.atlassian.jira.plugins.dvcs.spi.github.model;

/**
 * Line commit comment.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommitLineComment
{

    /**
     * @see #getId()
     */
    private int id;

    /**
     * @see #getGitHubId()
     */
    private long gitHubId;

    /**
     * @see #getCommit()
     */
    private GitHubCommit commit;

    /**
     * @see #getPath()
     */
    private String path;

    /**
     * @see #getLine()
     */
    private int line;

    private String text;

    /**
     * Constructor.
     */
    public GitHubCommitLineComment()
    {
    }

    /**
     * @return Identity of this object.
     */
    public int getId()
    {
        return id;
    }

    /**
     * @param id
     *            {@link #getId()}
     */
    public void setId(int id)
    {
        this.id = id;
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
