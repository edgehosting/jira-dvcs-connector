package com.atlassian.jira.plugins.dvcs.model;

import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "sync")
@XmlAccessorType(XmlAccessType.FIELD)
public class DefaultProgress implements Progress, Serializable
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
    private Long startTime;

    @XmlAttribute
    private Long finishTime;

    @XmlAttribute
    private String error;

    @XmlAttribute
    private String errorTitle;

    @XmlAttribute
    private Date firstMessageTime;

    @XmlAttribute
    private int flightTimeMs;

    @XmlAttribute
    private int numRequests;

    @XmlElement
    private List<SmartCommitError> smartCommitErrors = new ArrayList<SmartCommitError>();

    @XmlAttribute
    private boolean softsync;

    @XmlAttribute
    private boolean webHookSync;

    @XmlAttribute
    private boolean warning;

    @XmlTransient
    private boolean hasAdminPermission = true;

    @XmlTransient
    @Deprecated
    // to be removed
    private boolean shouldStop = false;

    @XmlTransient
    private int auditLogId;

    @XmlTransient
    private EnumSet<SynchronizationFlag> runAgain;

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

    @Override
    public void setError(String error)
    {
        this.error = error;
    }

    @Override
    public EnumSet<SynchronizationFlag> getRunAgainFlags()
    {
        return runAgain;
    }

    @Override
    public void setRunAgainFlags(final EnumSet<SynchronizationFlag> runAgain)
    {
        this.runAgain = runAgain;
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

    @Override
    public Long getStartTime()
    {
        return startTime;
    }

    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    @Override
    public Long getFinishTime()
    {
        return finishTime;
    }

    @Override
    public Date getFirstMessageTime()
    {
        return this.firstMessageTime;
    }

    @Override
    public void incrementRequestCount(final Date messageTime)
    {
        if (this.firstMessageTime == null)
        {
            this.firstMessageTime = messageTime;
        }
        this.numRequests++;
    }

    @Override
    public void addFlightTimeMs(int timeMs)
    {
        this.flightTimeMs += timeMs;
    }

    @Override
    public int getNumRequests()
    {
        return this.numRequests;
    }

    @Override
    public int getFlightTimeMs()
    {
        return this.flightTimeMs;
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

    @Override
    public int getAuditLogId()
    {
        return auditLogId;
    }

    @Override
    public void setAuditLogId(int auditLogId)
    {
        this.auditLogId = auditLogId;
    }

    @Override
    public boolean isSoftsync()
    {
        return softsync;
    }

    @Override
    public void setSoftsync(final boolean softsync)
    {
        this.softsync = softsync;
    }

    public boolean isWebHookSync()
    {
        return webHookSync;
    }

    public void setWebHookSync(final boolean webHookSync)
    {
        this.webHookSync = webHookSync;
    }

    @Override
    public boolean isWarning()
    {
        return warning;
    }

    @Override
    public void setWarning(final boolean value)
    {
        this.warning = value;
    }

    @Override
    public String getErrorTitle()
    {
        return errorTitle;
    }

    @Override
    public void setErrorTitle(final String errorTitle)
    {
        this.errorTitle = errorTitle;
    }
}
