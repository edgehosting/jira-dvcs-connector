package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

public class BitbucketPullRequestCommentActivityInline implements Serializable 
{
	private static final long serialVersionUID = -4382573167292917291L;

	private String path;

	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
}
