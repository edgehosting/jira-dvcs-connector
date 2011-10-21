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

    public SyncProgress()
    {
    }

    public SyncProgress(boolean isFinished)
    {
        this.isFinished = isFinished;
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
