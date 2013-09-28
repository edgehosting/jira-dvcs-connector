package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;

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
}
