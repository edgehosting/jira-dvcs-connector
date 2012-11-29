package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.project.DeleteProjectPage;
import com.atlassian.jira.pageobjects.project.ViewProjectsPage;
import com.atlassian.jira.pageobjects.project.summary.ProjectSummaryPageTab;

/**
 * @author Martin Skurla
 */
public class JiraPageUtils
{
    private JiraPageUtils() {}


    public static boolean jiraProjectExists(JiraTestedProduct jira, String projectName)
    {
        ViewProjectsPage viewProjectsPage = jira.getPageBinder().navigateToAndBind(ViewProjectsPage.class);

        return viewProjectsPage.isRowPresent(projectName);
    }

    public static void deleteProject(JiraTestedProduct jira, String projectKey)
    {
        ProjectSummaryPageTab projectSummaryPageTab = jira.getPageBinder().navigateToAndBind(ProjectSummaryPageTab.class,
                                                                                             projectKey);
        long projectId = projectSummaryPageTab.getProjectId();

        jira.getPageBinder().navigateToAndBind(DeleteProjectPage.class, projectId).submitConfirm();
    }
}
