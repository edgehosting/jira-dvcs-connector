package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.components.JiraHeader;
import com.atlassian.jira.pageobjects.dialogs.quickedit.CreateIssueDialog;
import com.atlassian.jira.pageobjects.pages.DashboardPage;
import com.atlassian.jira.pageobjects.project.DeleteProjectPage;
import com.atlassian.jira.pageobjects.project.ViewProjectsPage;
import com.atlassian.jira.pageobjects.project.summary.ProjectSummaryPageTab;
/**
 * @author Martin Skurla
 */
public class JiraPageUtils
{
    private JiraPageUtils() {}


    public static boolean projectExists(JiraTestedProduct jira, String projectName)
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

    public static void createIssue(JiraTestedProduct jira, String projectName)
    {
        JiraHeader jiraHeader = jira.getPageBinder().navigateToAndBind(DashboardPage.class).getHeader();
        CreateIssueDialog createIssueDialog = jiraHeader.createIssue();

        createIssueDialog.selectProject(projectName);
        try
        {
            Thread.sleep(1000L);
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
        }

        createIssueDialog.fill("summary", "Missing commits fix demonstration");
        createIssueDialog.submit(DashboardPage.class);
    }
    
    public static void checkSyncProcessSuccess()
    {
        do
        {
            sleep(1000);
        } while (!isSyncFinished());
    }

    private static boolean isSyncFinished()
    {
        //PFFFF
        /*
        RepositoryList repositories = new RepositoriesLocalRestpoint().getRepositories();
        for (Repository repository : repositories.getRepositories()) {
            if (repository.getSync() != null && !repository.getSync().isFinished()) {
                return false;
            }
        }*/
        return true;
    }

    private static void sleep(long milis)
    {
        try
        {
            Thread.sleep(milis);
        } catch (InterruptedException e)
        {
            // ignore
        }
    }
}
