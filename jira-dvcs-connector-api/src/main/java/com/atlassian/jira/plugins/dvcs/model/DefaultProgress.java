package com.atlassian.jira.plugins.dvcs.model;


import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DefaultProgress implements Progress, ProgressWriter
{
	private boolean finished = false;
	
	private int changesetCount = 0;
	private int jiraCount = 0;
	private int synchroErrorCount = 0;
	private long startTime = 0;
	private String error;
	
	public DefaultProgress()
	{
		super();
	}

    @Override
    public void inProgress(int changesetCount, int jiraCount, int synchroErrorCount)
    {
    	this.changesetCount = changesetCount;
    	this.jiraCount = jiraCount;
        this.synchroErrorCount = synchroErrorCount;
    }

    public void queued () {
    	// not used
    }

    public void start()
    {
    	startTime = System.currentTimeMillis();
    }

	public void finish()
	{
		finished = true;
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

	@Override
    public boolean isFinished()
	{
		return finished;
	}

	public long getStartTime()
	{
		return startTime;
	}

	public void setStartTime(long startTime)
	{
		this.startTime = startTime;
	}

	public void setFinished(boolean finished)
	{
		this.finished = finished;
	}

	public void setChangesetCount(int changesetCount)
	{
		this.changesetCount = changesetCount;
	}

	public void setJiraCount(int jiraCount)
	{
		this.jiraCount = jiraCount;
	}

	public void setSynchroErrorCount(int synchroErrorCount)
	{
		this.synchroErrorCount = synchroErrorCount;
	}
}