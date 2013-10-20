package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.BaseProgressEnabledMessage;

/**
 * Message which is fired when a changeset should be synchronized.
 *
 * @see #getRefreshAfterSynchronizedAt()
 * @author Stanislav Dvorscak
 *
 */
public class BitbucketSynchronizeChangesetMessage extends BaseProgressEnabledMessage implements Serializable
{

    private static final long serialVersionUID = 1L;

    private Date refreshAfterSynchronizedAt;

    private List<BranchHead> newHeads;

    private List<String> exclude;

    private int page;

    private Map<String, String> nodesToBranches;

    public BitbucketSynchronizeChangesetMessage(Repository repository, Date refreshAfterSynchronizedAt,
            Progress progress, boolean softSync, int syncAuditId)
    {
        this(repository, refreshAfterSynchronizedAt, progress, null, null, 1, null, softSync, syncAuditId);
    }

    public BitbucketSynchronizeChangesetMessage(Repository repository, Date refreshAfterSynchronizedAt,
            Progress progress, List<BranchHead> newHeads, List<String> exclude, int page, Map<String, String> nodesToBranches, boolean softSync, int syncAuditId)
    {
        super(progress, syncAuditId, softSync, repository);
        this.refreshAfterSynchronizedAt = refreshAfterSynchronizedAt;
        this.newHeads = newHeads;
        this.exclude = exclude;
        this.page = page;
        this.nodesToBranches = nodesToBranches;
    }

    public Date getRefreshAfterSynchronizedAt()
    {
        return refreshAfterSynchronizedAt;
    }

    public List<String> getExclude()
    {
        return exclude;
    }

    public int getPage()
    {
        return page;
    }

    public List<BranchHead> getNewHeads()
    {
        return newHeads;
    }

    public Map<String, String> getNodesToBranches()
    {
        return nodesToBranches;
    }

}
