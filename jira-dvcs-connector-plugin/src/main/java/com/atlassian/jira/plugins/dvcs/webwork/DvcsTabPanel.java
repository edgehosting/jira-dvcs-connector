package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;

public class DvcsTabPanel extends AbstractIssueTabPanel
{
    private final Logger logger = LoggerFactory.getLogger(DvcsTabPanel.class);

    private static final GenericMessageAction DEFAULT_MESSAGE = new GenericMessageAction("No commits found.");

    private final ChangesetService changesetService;
    private final ChangesetRenderer changesetRenderer;

    private final PermissionManager permissionManager;
    private final RepositoryService repositoryService;

    public DvcsTabPanel(ChangesetService changesetService, ChangesetRenderer changesetRenderer, PermissionManager permissionManager, RepositoryService repositoryService)
    {
        this.changesetService = changesetService;
        this.changesetRenderer = changesetRenderer;
        this.permissionManager = permissionManager;
        this.repositoryService = repositoryService;
    }

    @Override
    public List<IssueAction> getActions(Issue issue, User user)
    {
        String issueKey = issue.getKey();
        List<IssueAction> bitbucketActions = new ArrayList<IssueAction>();
        try
        {
            for (Changeset changeset : changesetService.getByIssueKey(issueKey))
            {
                logger.debug("found changeset [ {} ] on issue [ {} ]", changeset.getNode(), issueKey);

                String changesetAsHtml = changesetRenderer.getHtmlForChangeset(changeset);
                if (StringUtils.isNotBlank(changesetAsHtml))
                {
                    bitbucketActions.add(new CommitsIssueAction(changesetAsHtml, changeset.getDate()));
                }
            }
        } catch (SourceControlException e)
        {
            logger.debug("Could not retrieve changeset for [ " + issueKey + " ]: " + e, e);
        }

        if (bitbucketActions.isEmpty())
            bitbucketActions.add(DEFAULT_MESSAGE);

        return bitbucketActions;
    }

    @Override
    public boolean showPanel(Issue issue, User user)
    {
        return permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user) &&
                repositoryService.existsLinkedRepositories();
    }

}

