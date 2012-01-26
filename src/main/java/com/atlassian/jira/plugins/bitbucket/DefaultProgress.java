package com.atlassian.jira.plugins.bitbucket;


import com.atlassian.jira.plugins.bitbucket.api.Progress;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;

public class DefaultProgress implements Progress, ProgressWriter
{
	private boolean isFinished = false;
	
	int changesetCount = 0;
	int jiraCount = 0;
	int synchroErrorCount = 0;
	long startTime = 0;
	private String error;

    @Override
    public void inProgress(int changesetCount, int jiraCount, int synchroErrorCount)
    {
    	this.changesetCount = changesetCount;
    	this.jiraCount = jiraCount;
        this.synchroErrorCount = synchroErrorCount;
    }

    public void start()
    {
    	startTime = System.currentTimeMillis();
    }

	public void finish()
	{
		isFinished = true;
	}

	@Override
    public int getChangesetCount()
    {
    	return changesetCount;
    }
    
    @Override
    public int getJiraCount()
    {
    	return jiraCount;
    }

    @Override
    public int getSynchroErrorCount()
    {
        return synchroErrorCount;
    }

    public void setError(String error)
	{
		this.error = error;
	}
	
	@Override
    public String getError()
	{
		return error;
	}

	public void queued()
	{
	    // not used
	}

	@Override
    public boolean isFinished()
	{
		return isFinished;
	}
}