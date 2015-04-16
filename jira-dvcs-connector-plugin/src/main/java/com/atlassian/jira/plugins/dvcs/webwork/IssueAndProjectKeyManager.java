package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;

import java.util.Set;

/**
 * Interface to retrieve historical issue a project keys for issue
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
public interface IssueAndProjectKeyManager
{
    Set<String> getAllIssueKeys(Issue issue);

    Set<String> getAllProjectKeys(Project project);

    Set<String> getAllIssueKeys(String issueKey);

    Set<String> getAllProjectKeys(String projectKey);

    /**
     * Checks permissions on an issue for the logged in user
     *
     * @param permissionKey Permission to be checked
     * @param issue issue
     *
     * @return true if the logged in user has required permision, false otherwise
     */
    boolean hasIssuePermission(ProjectPermissionKey permissionKey, Issue issue);

    /**
     * Checks permissions on a project for the logged in user
     *
     * @param permissionKey Permission to be checked
     * @param project project
     *
     * @return true if the logged in user has required permision, false otherwise
     */
    boolean hasProjectPermission(ProjectPermissionKey permissionKey, Project project);

    Issue getIssue(String issueKey);

    Project getProject(String projectKey);
}
