package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.Permissions;

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
     * @param permission Permission to be checked
     * @param issueKey issue key
     *
     * @return true if the logged in user has required permision, false otherwise
     */
    boolean hasIssuePermission(Permissions.Permission permission, String issueKey);

    /**
     * Checks permissions on a project for the logged in user
     *
     * @param permission Permission to be checked
     * @param projectKey project key
     *
     * @return true if the logged in user has required permision, false otherwise
     */
    boolean hasProjectPermission(Permissions.Permission permission, String projectKey);
}
