package com.atlassian.jira.plugins.dvcs.spi.github.message;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.BaseProgressEnabledMessage;

import java.io.Serializable;
import java.util.Date;

/**
 * Message which is fired when a changeset should be synchronized.
 *
 * @see #getRefreshAfterSynchronizedAt()
 * @author Stanislav Dvorscak
 *
 */
public class SynchronizeChangesetMessage extends BaseProgressEnabledMessage implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final String branch;

    private final String node;

    private Date refreshAfterSynchronizedAt;

    public SynchronizeChangesetMessage(Repository repository, String branch, String node, Date refreshAfterSynchronizedAt,
            Progress progress, boolean softSync, int syncAuditId, boolean webHookSync)
    {
        super(progress, syncAuditId, softSync, repository, webHookSync);
        this.branch = branch;
        this.node = node;
        this.refreshAfterSynchronizedAt = refreshAfterSynchronizedAt;
    }

    public String getBranch()
    {
        return branch;
    }

    public String getNode()
    {
        return node;
    }

    public Date getRefreshAfterSynchronizedAt()
    {
        return refreshAfterSynchronizedAt;
    }

}
