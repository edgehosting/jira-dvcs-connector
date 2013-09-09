package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

/**
 * Represents comment activity done over commit.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("ACTIVITY_C_COMMENT")
public interface RepositoryCommitCommentActivityMapping extends RepositoryCommitActivityMapping
{

    /**
     * @see #getRemoteId()
     */
    String REMOTE_ID = "REMOTE_ID";

    /**
     * @see #getRemoteParentId()
     */
    String REMOTE_PARENT_ID = "REMOTE_PARENT_ID";

    /**
     * @see #getCommentUrl()
     */
    String COMMENT_URL = "COMMENT_URL";

    /**
     * @see #getFile()
     */
    String FILE = "FILE";

    /**
     * @see #getMessage()
     */
    String MESSAGE = "MESSAGE";

    /**
     * @return Remote ID - id used inside provider.
     */
    long getRemoteId();

    /**
     * @param remoteId
     *            {@link #getRemoteId()}
     */
    void setRemoteId(long remoteId);

    /**
     * @return If it is replay comment - it targets to the {@link #getRemoteId()} of parent comment.
     */
    Integer getRemoteParentId();

    /**
     * @param remoteParentId
     *            {@link #getRemoteParentId()}
     */
    void setRemoteParentId(Integer remoteParentId);

    /**
     * @return URL of commit inside provider.
     */
    String getCommentUrl();

    /**
     * @param commentUrl
     *            {@link #getCommentUrl()}
     */
    void setCommentUrl(String commentUrl);

    /**
     * @return Name of a file, if it is about in-line comment, null otherwise.
     */
    String getFile();

    /**
     * @param file
     *            {@link #getFile()}
     */
    void setFile(String file);

    /**
     * @return Message assigned to commit.
     */
    @StringLength(StringLength.UNLIMITED)
    String getMessage();

    /**
     * @param message
     *            {@link #getMessage()}
     */
    void setMessage(String message);

}
