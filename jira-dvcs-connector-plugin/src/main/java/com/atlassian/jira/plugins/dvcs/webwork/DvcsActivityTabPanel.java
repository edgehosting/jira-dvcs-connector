package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.webwork.render.DefaultIssueAction;
import com.atlassian.jira.plugins.dvcs.webwork.render.IssueActionFactory;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.templaterenderer.TemplateRenderer;

public class DvcsActivityTabPanel extends AbstractIssueTabPanel
{
    private final Logger logger = LoggerFactory.getLogger(DvcsActivityTabPanel.class);

    private static final GenericMessageAction DEFAULT_MESSAGE = new GenericMessageAction("No pull requests found.");
    private final PermissionManager permissionManager;
    private final RepositoryService repositoryService;
    private final RepositoryActivityDao activityDao;
    private final IssueActionFactory issueActionFactory;
    private final TemplateRenderer templateRenderer;

    private static final Comparator<? super IssueAction> ISSUE_ACTION_COMPARATOR = new Comparator<IssueAction>()
    {
        @Override
        public int compare(IssueAction o1, IssueAction o2)
        {
            DefaultIssueAction o1d = (DefaultIssueAction) o1;
            DefaultIssueAction o2d = (DefaultIssueAction) o2;
            if (o1 == null || o1.getTimePerformed() == null)
                return -1;
            if (o2 == null || o2.getTimePerformed() == null)
                return 1;
            return new Integer(o1d.getId()).compareTo(o2d.getId());
        }
    };

    public DvcsActivityTabPanel(PermissionManager permissionManager,
            RepositoryService repositoryService, RepositoryActivityDao activityDao,
            @Qualifier("aggregatedIssueActionFactory") IssueActionFactory issueActionFactory, TemplateRenderer templateRenderer)
    {
        this.permissionManager = permissionManager;
        this.repositoryService = repositoryService;
        this.activityDao = activityDao;
        this.issueActionFactory = issueActionFactory;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public List<IssueAction> getActions(Issue issue, User user)
    {
        String issueKey = issue.getKey();
        List<IssueAction> issueActions = new ArrayList<IssueAction>();

        //
        List<RepositoryPullRequestMapping> prs = activityDao.getPullRequestsForIssue(issueKey);
        issueActions.add(new DefaultIssueAction(templateRenderer, "/templates/activity/pr-view.vm", EasyMap.build("prs", prs), new Date()));
        //
        //
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
