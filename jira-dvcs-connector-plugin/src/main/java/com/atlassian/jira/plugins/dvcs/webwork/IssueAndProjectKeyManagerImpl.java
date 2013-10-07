package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;

import java.util.Collections;
import java.util.Set;

/**
 * Utility class to retrieve historical issue a project keys for issue
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
public class IssueAndProjectKeyManagerImpl implements IssueAndProjectKeyManager
{
    private final IssueManager issueManager;
    private final ChangeHistoryManager changeHistoryManager;
    private final ProjectManager projectManager;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext authenticationContext;

    public IssueAndProjectKeyManagerImpl(final IssueManager issueManager, final ChangeHistoryManager changeHistoryManager, final ProjectManager projectManager, final PermissionManager permissionManager, final JiraAuthenticationContext authenticationContext)
    {
        this.issueManager = issueManager;
        this.changeHistoryManager = changeHistoryManager;
        this.projectManager = projectManager;
        this.permissionManager = permissionManager;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public Set<String> getAllIssueKeys(Issue issue)
    {
        if (issue == null)
        {
            return Collections.emptySet();
        }
        return SystemUtils.getAllIssueKeys(issueManager, changeHistoryManager, issue);
    }

    @Override
    public Issue getIssue(String issueKey)
    {
        return issueManager.getIssueObject(issueKey);
    }

    @Override
    public Project getProject(String projectKey)
    {
        return projectManager.getProjectObjByKey(projectKey);
    }

    @Override
    public Set<String> getAllIssueKeys(String issueKey)
    {
        MutableIssue issue = issueManager.getIssueObject(issueKey);
        return getAllIssueKeys(issue);
    }

    @Override
    public Set<String> getAllProjectKeys(Project project)
    {
        return SystemUtils.getAllProjectKeys(projectManager, project);
    }

    @Override
    public Set<String> getAllProjectKeys(String projectKey)
    {
        return SystemUtils.getAllProjectKeys(projectManager, projectManager.getProjectObjByKey(projectKey));
    }

    @Override
    public boolean hasIssuePermission(Permissions.Permission permission, Issue issue)
    {
        if (issue == null)
        {
            throw new NullPointerException("The issue cannot be null");
        }
        User loggedInUser = authenticationContext.getLoggedInUser();
        return permissionManager.hasPermission(permission.getId(), issue, loggedInUser);
    }

    @Override
    public boolean hasProjectPermission(Permissions.Permission permission, Project project)
    {
        if (project == null)
        {
            throw new NullPointerException("The project cannot be null");
        }
        User loggedInUser = authenticationContext.getLoggedInUser();
        return permissionManager.hasPermission(permission.getId(), project, loggedInUser);
    }
}
