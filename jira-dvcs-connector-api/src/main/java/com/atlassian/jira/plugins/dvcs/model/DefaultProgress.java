package com.atlassian.jira.plugins.dvcs.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "sync")
@XmlAccessorType(XmlAccessType.FIELD)
public class DefaultProgress implements Progress
{

    @XmlAttribute
    private boolean finished = false;

    @XmlAttribute
    private int changesetCount = 0;

    @XmlAttribute
    private int jiraCount = 0;

    @XmlAttribute
    private int pullRequestActivityCount = 0;

    @XmlAttribute
    private int synchroErrorCount = 0;

    @XmlAttribute
    private long startTime = 0;

    @XmlAttribute
    private long finishTime = 0;

    @XmlAttribute
    private String error;

    @XmlElement
    private List<SmartCommitError> smartCommitErrors = new ArrayList<SmartCommitError>();

    @XmlTransient
    private boolean hasAdminPermission = true;

    @XmlTransient
    @Deprecated
    // to be removed
    private boolean shouldStop = false;

    @XmlTransient
    private int auditLogId;

    public DefaultProgress()
    {
        super();
    }

    @Override
    // TODO remove synchroErrorCount
    public void inProgress(int changesetCount, int jiraCount, int synchroErrorCount)
    {
        this.changesetCount = changesetCount;
        this.jiraCount = jiraCount;
        this.synchroErrorCount = synchroErrorCount;
    }

    @Override
    public void inPullRequestProgress(int pullRequestActivityCount, int jiraCount)
    {
        this.pullRequestActivityCount = pullRequestActivityCount;
        this.jiraCount = jiraCount;
    }

    public void queued()
    {
        // not used, maybe one day we can have special icon for this state
    }

    public void start()
    {
        startTime = System.currentTimeMillis();
        smartCommitErrors.clear();
    }

    @Override
    public void finish()
    {
        finishTime = System.currentTimeMillis();
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
    public int getPullRequestActivityCount()
    {
        return pullRequestActivityCount;
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

    public long getFinishTime()
    {
        return finishTime;
    }

    public void setFinishTime(long finishTime)
    {
        this.finishTime = finishTime;
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

    public void setPullRequestActivityCount(int pullRequestActivityCount)
    {
        this.pullRequestActivityCount = pullRequestActivityCount;
    }

    public void setSynchroErrorCount(int synchroErrorCount)
    {
        this.synchroErrorCount = synchroErrorCount;
    }

    @Override
    public List<SmartCommitError> getSmartCommitErrors()
    {
        return smartCommitErrors;
    }

    @Override
    public void setSmartCommitErrors(List<SmartCommitError> smartCommitErrors)
    {
        this.smartCommitErrors = smartCommitErrors;
    }

    @Override
    public boolean isShouldStop()
    {
        return shouldStop;
    }

    @Override
    public void setShouldStop(boolean shouldStop)
    {
        this.shouldStop = shouldStop;
    }

    @Override
    public boolean hasAdminPermission()
    {
        return hasAdminPermission;
    }

    @Override
    public void setAdminPermission(boolean hasAdminPermission)
    {
        this.hasAdminPermission = hasAdminPermission;
    }

    public int getAuditLogId()
    {
        return auditLogId;
    }

    public void setAuditLogId(int auditLogId)
    {
        this.auditLogId = auditLogId;
    }
}