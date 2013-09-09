package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.page.dashboard.CreateIssueDialog;
import it.restart.com.atlassian.jira.plugins.dvcs.page.dashboard.DashboardPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuesPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuesPageIssueRow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.TestedProductFactory;

/**
 * Abstract test for all DVCS tests.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class AbstractDVCSTest
{

    /**
     * @see #getJiraTestedProduct()
     */
    private JiraTestedProduct jiraTestedProduct;

    /**
     * Holds information about created test issues.
     */
    private Map<String, Map<String, String>> projectKeyAndIssueSummaryToIssueKey = new HashMap<String, Map<String, String>>();


    protected OAuth oAuth;
    /**
     * Prepares common test environment.
     */
    @BeforeClass
    public void onTestsEnvironmentSetup()
    {
        jiraTestedProduct = TestedProductFactory.create(JiraTestedProduct.class);
        new JiraLoginPageController(jiraTestedProduct).login();
    }

    /**
     * Destroys test environment.
     */
    @AfterMethod(alwaysRun = true)
    public void onTestCleanUp()
    {
        for (Entry<String, Map<String, String>> byProjectKey : projectKeyAndIssueSummaryToIssueKey.entrySet())
        {
            String projectKey = byProjectKey.getKey();
            for (String issueSummary : byProjectKey.getValue().keySet())
            {
                deleteTestIssue(projectKey, issueSummary);
            }
        }
    }

    /**
     * @return JIRA, which is used for testing.
     */
    protected JiraTestedProduct getJiraTestedProduct()
    {
        return jiraTestedProduct;
    }

    /**
     * Adds test issue with provided informations.
     * 
     * @param projectKey
     *            project key, to which will be assigned issue
     * @param issueSummary
     *            summary of created issue e.g.: name of test class, or something unique
     * @return key of created issue
     */
    protected String addTestIssue(String projectKey, String issueSummary)
    {
        deleteTestIssue(projectKey, issueSummary);
        return createTestIssue(projectKey, issueSummary);
    }

    /**
     * Creates test issue with provided information.
     * 
     * @param projectKey
     *            project key, to which will be assigned issue
     * @param issueSummary
     *            summary of created issue e.g.: name of test class, or something unique
     * @return key of created issue
     */
    private String createTestIssue(String projectKey, String issueSummary)
    {
        // creates issue for testing
        DashboardPage dashboardPage = getJiraTestedProduct().visit(DashboardPage.class);
        dashboardPage.createIssue();
        CreateIssueDialog issueDialog = dashboardPage.getCreateIssueDialog();
        issueDialog.fill(issueSummary);
        issueDialog.create();

        // gets key of created issue
        IssuesPage issuesPage = getJiraTestedProduct().visit(IssuesPage.class);
        issuesPage = getJiraTestedProduct().visit(IssuesPage.class);
        issuesPage.fillSearchForm(projectKey, issueSummary);
        List<IssuesPageIssueRow> issueRows = issuesPage.getIssueRows();
        Assert.assertEquals(issueRows.size(), 1);
        String result = issueRows.get(0).getIssueKey();

        Map<String, String> byProjectKey = projectKeyAndIssueSummaryToIssueKey.get(projectKey);
        if (byProjectKey == null)
        {
            projectKeyAndIssueSummaryToIssueKey.put(projectKey, byProjectKey = new HashMap<String, String>());
        }
        byProjectKey.put(issueSummary, result);
        return result;
    }

    /**
     * Deletes provided test issue
     * 
     * @param projectKey
     *            project key, to which will be assigned issue
     * @param issueSummary
     *            summary of created issue e.g.: name of test class, or something unique
     */
    private void deleteTestIssue(String projectKey, String issueSummary)
    {
        // deletes obsolete test issues
        IssuesPage issuesPage = getJiraTestedProduct().visit(IssuesPage.class);
        issuesPage.fillSearchForm(projectKey, issueSummary);
        issuesPage.deleteAll();

        Map<String, String> byProjectKey = projectKeyAndIssueSummaryToIssueKey.get(projectKey);
        if (byProjectKey != null)
        {
            byProjectKey.remove(issueSummary);
            if (byProjectKey.isEmpty())
            {
                projectKeyAndIssueSummaryToIssueKey.clear();
            }
        }
    }

    protected OAuthCredentials getOAuthCredentials()
    {
        return new OAuthCredentials(oAuth.key, oAuth.secret);
    }

}
