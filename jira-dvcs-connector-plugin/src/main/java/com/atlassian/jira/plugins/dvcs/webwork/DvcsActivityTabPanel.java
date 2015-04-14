package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.webwork.render.DefaultIssueAction;
import com.atlassian.jira.plugins.dvcs.webwork.render.IssueActionFactory;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.software.api.permissions.SoftwareProjectPermissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Currently unused, see BBC-930
 *
 * This class implements both the JIRA 6.X and 7.X versions of the {@link com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel}
 * so that it can be compiled and run against either.
 */
public class DvcsActivityTabPanel extends AbstractIssueTabPanel
{
    private final Logger logger = LoggerFactory.getLogger(DvcsActivityTabPanel.class);

    private static final GenericMessageAction DEFAULT_MESSAGE = new GenericMessageAction("No pull requests found.");
    private final PermissionManager permissionManager;
    private final RepositoryService repositoryService;
    private final RepositoryPullRequestDao activityDao;
    private final IssueActionFactory issueActionFactory;
    private final TemplateRenderer templateRenderer;
    private final IssueAndProjectKeyManager issueAndProjectKeyManager;

    private static final Comparator<? super IssueAction> ISSUE_ACTION_COMPARATOR = new Comparator<IssueAction>()
    {
        @Override
        public int compare(IssueAction o1, IssueAction o2)
        {
            DefaultIssueAction o1d = (DefaultIssueAction) o1;
            DefaultIssueAction o2d = (DefaultIssueAction) o2;
            if (o1 == null || o1.getTimePerformed() == null)
            {
                return -1;
            }
            if (o2 == null || o2.getTimePerformed() == null)
            {
                return 1;
            }
            return new Integer(o1d.getId()).compareTo(o2d.getId());
        }
    };

    public DvcsActivityTabPanel(@ComponentImport PermissionManager permissionManager,
            RepositoryService repositoryService, RepositoryPullRequestDao activityDao,
            @Qualifier ("aggregatedIssueActionFactory") IssueActionFactory issueActionFactory,
            @ComponentImport TemplateRenderer templateRenderer, IssueAndProjectKeyManager issueAndProjectKeyManager)
    {
        this.permissionManager = checkNotNull(permissionManager);
        this.repositoryService = checkNotNull(repositoryService);
        this.activityDao = checkNotNull(activityDao);
        this.issueActionFactory = checkNotNull(issueActionFactory);
        this.templateRenderer = checkNotNull(templateRenderer);
        this.issueAndProjectKeyManager = checkNotNull(issueAndProjectKeyManager);
    }

    public List<IssueAction> getActions(Issue issue, User user)
    {
        return getActions(issue, ApplicationUsers.from(user));
    }

    public List<IssueAction> getActions(Issue issue, ApplicationUser user)
    {
        String issueKey = issue.getKey();
        Set<String> issueKeys = issueAndProjectKeyManager.getAllIssueKeys(issue);
        List<IssueAction> issueActions = new ArrayList<IssueAction>();

        List<RepositoryPullRequestMapping> prs = activityDao.getByIssueKeys(issueKeys);
        Map<String, Object> ctxt = Maps.newHashMap(ImmutableMap.<String, Object>of("prs", prs));
        issueActions.add(new DefaultIssueAction(templateRenderer, "/templates/activity/pr-view.vm", ctxt, new Date()));
        if (issueActions.isEmpty())
        {
            issueActions.add(DEFAULT_MESSAGE);
        }

        Collections.sort(issueActions, ISSUE_ACTION_COMPARATOR);
        return issueActions;
    }

    public boolean showPanel(Issue issue, User user)
    {
        return showPanel(issue, ApplicationUsers.from(user));
    }

    public boolean showPanel(Issue issue, ApplicationUser user)
    {
        return permissionManager.hasPermission(SoftwareProjectPermissions.VIEW_DEV_TOOLS, issue, user)
                && repositoryService.existsLinkedRepositories();
    }

}
