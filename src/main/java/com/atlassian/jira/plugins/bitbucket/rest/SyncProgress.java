package com.atlassian.jira.plugins.bitbucket.rest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "sync")
@XmlAccessorType(XmlAccessType.FIELD)
public class SyncProgress
{
    @XmlAttribute
    private boolean isFinished;

    @XmlAttribute
    private int changesetCount;

    @XmlAttribute
	private int jiraCount;

    @XmlAttribute
	private String error;

	public SyncProgress()
    {
    }

    
    public SyncProgress(boolean isFinished, int changesetCount, int jiraCount, String error)
	{
		this.isFinished = isFinished;
		this.changesetCount = changesetCount;
		this.jiraCount = jiraCount;
		this.error = error;
	}


	public int getChangesetCount()
	{
		return changesetCount;
	}

	public void setChangesetCount(int changesetCount)
	{
		this.changesetCount = changesetCount;
	}

	public int getJiraCount()
	{
		return jiraCount;
	}

	public void setJiraCount(int jiraCount)
	{
		this.jiraCount = jiraCount;
	}

	public String getError()
	{
		return error;
	}

	public void setError(String error)
	{
		this.error = error;
	}

	public boolean isFinished()
	{
		return isFinished;
	}

	public void setFinished(boolean isFinished)
	{
		this.isFinished = isFinished;
	}

}
