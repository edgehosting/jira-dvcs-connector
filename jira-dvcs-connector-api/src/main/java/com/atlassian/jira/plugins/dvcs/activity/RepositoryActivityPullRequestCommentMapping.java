package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Preload;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Preload
@Table("PR_COMMENT")
public interface RepositoryActivityPullRequestCommentMapping extends RepositoryActivityPullRequestMapping
{
    String MESSAGE = "MESSAGE";
    String COMMENT_URL = "COMMENT_URL";
    String REMOTE_ID = "REMOTE_ID";
    String REMOTE_PARENT_ID = "REMOTE_PARENT_ID";
    String FILE = "FILE";
    
    //
    // getters
    //
    @StringLength(StringLength.UNLIMITED)
    String getMessage();
    String getCommentUrl();
    Integer getRemoteParentId();
    int getRemoteId();
    String getFile();
    
    //
    // setters
    //
    @StringLength(StringLength.UNLIMITED)
    void setMessage(String message);
    void setCommentUrl(String commentUrl);
    void setRemoteParentId(Integer remoteParentId);
    void setRemoteId(int remoteId);
    void setFile(String file);
}

