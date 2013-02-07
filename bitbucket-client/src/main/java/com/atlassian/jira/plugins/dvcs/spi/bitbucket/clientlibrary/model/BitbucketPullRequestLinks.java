package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * 
 * @author mstencel@atlassian.com
 * 
 */
public class BitbucketPullRequestLinks implements Serializable {
	private static final long serialVersionUID = -8503751637022859413L;

	private String href;
	private String htmlHref;
	private String commitsHref;
	private String approvalsHref;
	private String diffHref;
	private String commentsHref;
	private String activityHref;

	public String getHref()
	{
		return href;
	}

	public void setHref(String href)
	{
		this.href = href;
	}

	public String getHtmlHref()
	{
		return htmlHref;
	}

	public void setHtmlHref(String htmlHref)
	{
		this.htmlHref = htmlHref;
	}

	public String getCommitsHref()
	{
		return commitsHref;
	}

	public void setCommitsHref(String commitsHref)
	{
		this.commitsHref = commitsHref;
	}

	public String getApprovalsHref()
	{
		return approvalsHref;
	}

	public void setApprovalsHref(String approvalsHref)
	{
		this.approvalsHref = approvalsHref;
	}

	public String getDiffHref()
	{
		return diffHref;
	}

	public void setDiffHref(String diffHref)
	{
		this.diffHref = diffHref;
	}

	public String getCommentsHref()
	{
		return commentsHref;
	}

	public void setCommentsHref(String commentsHref)
	{
		this.commentsHref = commentsHref;
	}

	public String getActivityHref()
	{
		return activityHref;
	}

	public void setActivityHref(String activityHref)
	{
		this.activityHref = activityHref;
	}
}
