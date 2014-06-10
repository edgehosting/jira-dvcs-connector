package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.BaseProgressEnabledMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    private List<String> include;
    private List<String> exclude;

    private BitbucketChangesetPage page;

    private Map<String, String> nodesToBranches;

    public BitbucketSynchronizeChangesetMessage(Repository repository, Date refreshAfterSynchronizedAt,
            Progress progress, List<String> include, List<String> exclude, BitbucketChangesetPage page,
            Map<String, String> nodesToBranches, boolean softSync, int syncAuditId, boolean webHookSync)
    {
        super(progress, syncAuditId, softSync, repository, webHookSync);
        this.refreshAfterSynchronizedAt = refreshAfterSynchronizedAt;
        this.include = include;
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

    public BitbucketChangesetPage getPage()
    {
        return page;
    }

    public List<String> getInclude()
    {
        return include;
    }

    public Map<String, String> getNodesToBranches()
    {
        return nodesToBranches;
    }

}
