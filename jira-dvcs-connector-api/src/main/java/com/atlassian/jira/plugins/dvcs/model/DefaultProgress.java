package com.atlassian.jira.plugins.dvcs.model;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "sync")
@XmlAccessorType(XmlAccessType.FIELD)
public class DefaultProgress implements Progress
{
	private volatile boolean shouldStop = false;
	
	@XmlAttribute
	private boolean finished = false;
	
	@XmlAttribute
	private int changesetCount = 0;
	
	@XmlAttribute
	private int jiraCount = 0;
	
	@XmlAttribute
	private int synchroErrorCount = 0;
	
	@XmlAttribute
	private long startTime = 0;
	
	@XmlAttribute
	private String error;
	
	public DefaultProgress()
	{
	}

    @Override
    public void inProgress(int changesetCount, int jiraCount, int synchroErrorCount)
    {
    	this.changesetCount = changesetCount;
    	this.jiraCount = jiraCount;
        this.synchroErrorCount = synchroErrorCount;
    }

    public void queued () {
    	// not used, maybe one day we can have special icon for this state 
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

	@Override
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

	@Override
    public boolean isShouldStop()
    {
    	return shouldStop;
    }

	public void setShouldStop(boolean shouldStop)
    {
    	this.shouldStop = shouldStop;
    }

}