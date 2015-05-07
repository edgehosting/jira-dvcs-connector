package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.compatibility.util.ApplicationUserUtil;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class to retrieve historical issue a project keys for issue
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
@Component
public class IssueAndProjectKeyManagerImpl implements IssueAndProjectKeyManager
{
    private final IssueManager issueManager;
    private final ChangeHistoryManager changeHistoryManager;
    private final ProjectManager projectManager;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext authenticationContext;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public IssueAndProjectKeyManagerImpl(@ComponentImport final IssueManager issueManager,
            @ComponentImport final ChangeHistoryManager changeHistoryManager,
            @ComponentImport final ProjectManager projectManager,
            @ComponentImport final PermissionManager permissionManager,
            @ComponentImport final JiraAuthenticationContext authenticationContext)
    {
        this.issueManager = checkNotNull(issueManager);
        this.changeHistoryManager = checkNotNull(changeHistoryManager);
        this.projectManager = checkNotNull(projectManager);
        this.permissionManager = checkNotNull(permissionManager);
        this.authenticationContext = checkNotNull(authenticationContext);
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
    public boolean hasIssuePermission(ProjectPermissionKey permissionKey, Issue issue)
    {
        if (issue == null)
        {
            throw new IllegalArgumentException("The issue cannot be null");
        }

        ApplicationUser user = authenticationContext.getUser();
        return permissionManager.hasPermission(permissionKey, issue, user);
    }

    @Override
    public boolean hasProjectPermission(ProjectPermissionKey permissionKey, Project project)
    {
        if (project == null)
        {
            throw new IllegalArgumentException("The project cannot be null");
        }

        ApplicationUser user = authenticationContext.getUser();
        return permissionManager.hasPermission(permissionKey, project, user);
    }
}
