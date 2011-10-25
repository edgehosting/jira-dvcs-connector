package com.atlassian.jira.plugins.bitbucket;


import com.atlassian.jira.plugins.bitbucket.api.Progress;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;

public class DefaultProgress implements Progress, ProgressWriter
{
	private boolean isFinished = false;
	private boolean isQueued = false;
	
	int changesetCount = 0;
	int jiraCount = 0;
	long startTime = 0;
	private String error;

    public void inProgress(int changesetCount, int jiraCount)
    {
    	this.changesetCount = changesetCount;
    	this.jiraCount = jiraCount;
    }

    public void start()
    {
    	startTime = System.currentTimeMillis();
    }

	public void finish()
	{
		isFinished = true;
	}

	public int getChangesetCount()
    {
    	return changesetCount;
    }
    
    public int getJiraCount()
    {
    	return jiraCount;
    }

	public void setError(String error)
	{
		this.error = error;
	}
	
	public String getError()
	{
		return error;
	}

	public void queued()
	{
		isQueued = true;
	}

	public boolean isFinished()
	{
		return isFinished;
	}

}