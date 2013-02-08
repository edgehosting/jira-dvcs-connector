package com.atlassian.jira.plugins.dvcs.activity;

/**
 * Represents single repository commit.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface RepositoryActivityCommitMapping
{

    /**
     * @return Author's username of the commit.
     */
    String getAuthor();

    /**
     * @param author
     *            {@link #getAuthor()}
     */
    void setAuthor(String author);

    /**
     * @return Author's full name of the commit, useful if the {@link #getAuthor()} username is not available.
     */
    String getAuthorName();

    /**
     * @param rawAuthor
     *            {@link #getAuthorName()}
     */
    void setAuthorName(String authorName);

    /**
     * @return Author's avatar URL, useful if the {@link #getAuthor()} username is not available. Can be null, then internal resolver will
     *         be used, otherwise it has precedence.
     */
    String getAuthorAvatarUrl();

    /**
     * @param authorAvatarUrl
     *            {@link #getAuthorAvatarUrl()}
     */
    void setAuthorAvatarUrl(String authorAvatarUrl);

    /**
     * @return Message of this commit.
     */
    String getMessage();

    /**
     * @param message
     *            {@link #getMessage()}
     */
    void setMessage(String message);

    /**
     * @return SHA/commit ID/hash ID of the commit.
     */
    String getNode();

    /**
     * @param node
     *            {@link #getNode()}
     */
    void setNode(String node);

}
