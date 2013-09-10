package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;

/**
 * Message which is fired when a changeset should be synchronized.
 * 
 * @see #getRefreshAfterSynchronizedAt()
 * @author Stanislav Dvorscak
 * 
 */
public class BitbucketSynchronizeChangesetMessage implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final Repository repository;

    private Date refreshAfterSynchronizedAt;

    private Progress progress;

    private final String synchronizationTag;

    private List<BranchHead> newHeads;

    private List<String> exclude;

    private int page;
    
    private Map<String, String> nodesToBranches;

    public BitbucketSynchronizeChangesetMessage(Repository repository, Date refreshAfterSynchronizedAt,
            Progress progress)
    {
        this(repository, refreshAfterSynchronizedAt, progress, null, null, null, 1, null);
    }

    public BitbucketSynchronizeChangesetMessage(Repository repository, Date refreshAfterSynchronizedAt,
            Progress progress, String synchronizationTag, List<BranchHead> newHeads, List<String> exclude, int page, Map<String, String> nodesToBranches)
    {
        this.repository = repository;
        this.refreshAfterSynchronizedAt = refreshAfterSynchronizedAt;
        this.progress = progress;
        this.synchronizationTag = synchronizationTag;
        this.newHeads = newHeads;
        this.exclude = exclude;
        this.page = page;
        this.nodesToBranches = nodesToBranches;
    }

    public Repository getRepository()
    {
        return repository;
    }

    public Date getRefreshAfterSynchronizedAt()
    {
        return refreshAfterSynchronizedAt;
    }

    public Progress getProgress()
    {
        return progress;
    }

    public String getSynchronizationTag()
    {
        return synchronizationTag;
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
