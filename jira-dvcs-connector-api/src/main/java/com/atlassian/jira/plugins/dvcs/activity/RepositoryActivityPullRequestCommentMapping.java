package com.atlassian.jira.plugins.dvcs.activity;

import net.java.ao.Preload;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Preload
@Table("PrComment")
public interface RepositoryActivityPullRequestCommentMapping extends RepositoryActivityPullRequestMapping
{
	String COMMENT_URL = "COMMENT_URL";
	String MESSAGE = "MESSAGE";

	//
    // getters
    //
    String getCommentUrl();
	@StringLength(StringLength.UNLIMITED)
    String getMessage();
	
    //
    // setters
    //
    void setCommentUrl(String url);
    @StringLength(StringLength.UNLIMITED)
    void setMessage(String message);
}

