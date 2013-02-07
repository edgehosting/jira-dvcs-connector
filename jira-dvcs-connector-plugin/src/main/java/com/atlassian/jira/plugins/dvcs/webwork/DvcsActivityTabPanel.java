package com.atlassian.jira.plugins.dvcs.webwork;

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
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.webwork.render.IssueActionFactory;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;

public class DvcsActivityTabPanel extends AbstractIssueTabPanel
{
    private final Logger logger = LoggerFactory.getLogger(DvcsActivityTabPanel.class);

    private static final GenericMessageAction DEFAULT_MESSAGE = new GenericMessageAction("No commits found.");
    private final PermissionManager permissionManager;
    private final ChangesetService changesetService;
    private final RepositoryService repositoryService;
    private final RepositoryActivityDao activityDao;

    private final IssueActionFactory issueActionFactory;

    public DvcsActivityTabPanel(PermissionManager permissionManager, ChangesetService changesetService,
            RepositoryService repositoryService, RepositoryActivityDao activityDao,
            @Qualifier("aggregatedIssueActionFactory") IssueActionFactory issueActionFactory)
    {
        this.permissionManager = permissionManager;
        this.changesetService = changesetService;
        this.repositoryService = repositoryService;
        this.activityDao = activityDao;
        this.issueActionFactory = issueActionFactory;
    }

    @Override
    public List<IssueAction> getActions(Issue issue, User user)
    {
        String issueKey = issue.getKey();
        List<IssueAction> bitbucketActions = new ArrayList<IssueAction>();
        
        try
        {
            List<RepositoryActivityPullRequestMapping> activities = activityDao.getRepositoryActivityForIssue(issueKey);
            
            for (RepositoryActivityPullRequestMapping activity : activities)
            {
            	logger.debug("found changeset [ {} ] on issue [ {} ]", activity.getID(), issueKey);
                IssueAction issueAction = issueActionFactory.create(activity);
                if (issueAction!=null)
                {
                    bitbucketActions.add(issueAction);
                }
            }
            
            for (Changeset changeset : changesetService.getByIssueKey(issueKey))
            {
                logger.debug("found changeset [ {} ] on issue [ {} ]", changeset.getNode(), issueKey);
                IssueAction issueAction = issueActionFactory.create(changeset);
                if (issueAction!=null)
                {
                    bitbucketActions.add(issueAction);
                }
            }
        } catch (SourceControlException e)
        {
            logger.debug("Could not retrieve changeset for [ " + issueKey + " ]: " + e, e);
        }

        if (bitbucketActions.isEmpty())
        {
            bitbucketActions.add(DEFAULT_MESSAGE);
        }
        
        return bitbucketActions;
    }

    @Override
    public boolean showPanel(Issue issue, User user)
    {
        return permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user) &&
                repositoryService.existsLinkedRepositories();
    }

}

