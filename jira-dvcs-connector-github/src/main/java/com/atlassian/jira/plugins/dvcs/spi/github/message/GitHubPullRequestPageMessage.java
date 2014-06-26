package com.atlassian.jira.plugins.dvcs.spi.github.message;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.BaseProgressEnabledMessage;

/**
 * Message for a particular GitHub pull request page
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
public class GitHubPullRequestPageMessage extends BaseProgressEnabledMessage
{
    private final int page;

    public GitHubPullRequestPageMessage(final Progress progress, final int syncAuditId, final boolean softSync, final Repository repository, final int page)
    {
        super(progress, syncAuditId, softSync, repository);
        this.page = page;
    }

    public int getPage()
    {
        return page;
    }
}
