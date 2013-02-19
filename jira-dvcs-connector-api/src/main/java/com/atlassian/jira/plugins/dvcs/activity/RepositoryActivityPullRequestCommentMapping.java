package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Preload;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Preload
@Table("PR_COMMENT")
public interface RepositoryActivityPullRequestCommentMapping extends RepositoryActivityPullRequestMapping
{
    String MESSAGE = "MESSAGE";
    String COMMENT_URL = "COMMENT_URL";
    String PARENT_ID = "PARENT_ID";
    String REMOTE_ID = "REMOTE_ID";
    
    //
    // getters
    //
    @StringLength(StringLength.UNLIMITED)
    String getMessage();
    String getCommentUrl();
    Integer getParentId();
    Integer getRemoteId();
    
    //
    // setters
    //
    @StringLength(StringLength.UNLIMITED)
    void setMessage(String message);
    void setCommentUrl(String commentUrl);
    void setParentId(int parentId);
    void setRemoteId(int remoteId);
}

