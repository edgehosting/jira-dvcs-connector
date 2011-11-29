package com.atlassian.jira.plugins.bitbucket.webwork;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;

public class BitbucketTabPanel extends AbstractIssueTabPanel
{
    private static final GenericMessageAction DEFAULT_MESSAGE = new GenericMessageAction("");
    private final PermissionManager permissionManager;
    private final Logger logger = LoggerFactory.getLogger(BitbucketTabPanel.class);
	private final RepositoryManager globalRepositoryManager;

    public BitbucketTabPanel(PermissionManager permissionManager, @Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager)
    {
        this.permissionManager = permissionManager;
        this.globalRepositoryManager = globalRepositoryManager;
    }

    @Override
    public List<IssueAction> getActions(Issue issue, User user)
    {
        String issueId = issue.getKey();
        List<IssueAction> bitbucketActions = new ArrayList<IssueAction>();
        try
        {
            for (Changeset changeset : globalRepositoryManager.getChangesets(issueId))
            {
                logger.debug("found changeset [ {} ] on issue [ {} ]", changeset.getNode(), issueId);
				SourceControlRepository repository = globalRepositoryManager.getRepository(changeset.getRepositoryId());
				String changesetAsHtml = globalRepositoryManager.getHtmlForChangeset(repository, changeset);
                bitbucketActions.add(new GenericMessageAction(changesetAsHtml));
            }
        }
        catch (com.atlassian.jira.plugins.bitbucket.api.SourceControlException e)
        {
            logger.debug("Could not retrieve changeset for [ " + issueId + " ]: " + e, e);
        }

        if (bitbucketActions.isEmpty())
            bitbucketActions.add(DEFAULT_MESSAGE);

        return bitbucketActions;
    }

	@Override
    public boolean showPanel(Issue issue, User user)
    {
        return permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user) &&
                !globalRepositoryManager.getRepositories(issue.getProjectObject().getKey()).isEmpty();
    }


}
