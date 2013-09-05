package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.*;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityMapping;
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
    private final IssueManager issueManager;
    private final ChangeHistoryManager changeHistoryManager;

    private final IssueActionFactory issueActionFactory;

    private static final Comparator<? super IssueAction> ISSUE_ACTION_COMPARATOR = new Comparator<IssueAction>()
    {
        @Override
        public int compare(IssueAction o1, IssueAction o2)
        {
            if (o1 == null || o1.getTimePerformed() == null)
                return -1;
            if (o2 == null || o2.getTimePerformed() == null)
                return 1;
            return o1.getTimePerformed().compareTo(o2.getTimePerformed());
        }
    };

    public DvcsActivityTabPanel(PermissionManager permissionManager, ChangesetService changesetService,
            RepositoryService repositoryService, RepositoryActivityDao activityDao,
            @Qualifier("aggregatedIssueActionFactory") IssueActionFactory issueActionFactory,
            IssueManager issueManager, ChangeHistoryManager changeHistoryManager)
    {
        this.permissionManager = permissionManager;
        this.changesetService = changesetService;
        this.repositoryService = repositoryService;
        this.activityDao = activityDao;
        this.issueActionFactory = issueActionFactory;
        this.issueManager = issueManager;
        this.changeHistoryManager = changeHistoryManager;
    }

    @Override
    public List<IssueAction> getActions(Issue issue, User user)
    {
        String issueKey = issue.getKey();
        List<IssueAction> issueActions = new ArrayList<IssueAction>();

        try
        {
            List<RepositoryActivityMapping> activities = activityDao.getRepositoryActivityForIssue(issueKey);

            for (RepositoryActivityMapping activity : activities)
            {
                logger.debug("found changeset [ {} ] on issue [ {} ]", activity.getID(), issueKey);
                IssueAction issueAction = issueActionFactory.create(activity);
                if (issueAction != null)
                {
                    issueActions.add(issueAction);
                }
            }

            Set<String> issueKeys = SystemUtils.getAllIssueKeys(issueManager, changeHistoryManager, issue);

            for (Changeset changeset : changesetService.getByIssueKey(issueKeys))
            {
                logger.debug("found changeset [ {} ] on issue [ {} ]", changeset.getNode(), issueKey);
                IssueAction issueAction = issueActionFactory.create(changeset);
                if (issueAction != null)
                {
                    issueActions.add(issueAction);
                }
            }

        } catch (SourceControlException e)
        {
            logger.debug("Could not retrieve changeset for [ " + issueKey + " ]: " + e, e);
        }

        if (issueActions.isEmpty())
        {
            issueActions.add(DEFAULT_MESSAGE);
        }

        Collections.sort(issueActions, ISSUE_ACTION_COMPARATOR);
        return issueActions;
    }

    @Override
    public boolean showPanel(Issue issue, User user)
    {
        return permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user)
                && repositoryService.existsLinkedRepositories();
    }

}
