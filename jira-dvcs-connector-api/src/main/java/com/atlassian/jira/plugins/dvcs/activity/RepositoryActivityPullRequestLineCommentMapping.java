package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Preload;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Preload
@Table("PR_LINE_COMMENT")
public interface RepositoryActivityPullRequestLineCommentMapping extends RepositoryActivityPullRequestMapping
{
    String COMMENT_URL = "COMMENT_URL";
    String MESSAGE = "MESSAGE";

    //
    // getters
    //
    @NotNull
    String getCommentUrl();

    @StringLength(StringLength.UNLIMITED)
    String getMessage();

    //
    // setters
    //
    
    void setCommentUrl(String commentUrl);

    void setMessage(String message);
}
