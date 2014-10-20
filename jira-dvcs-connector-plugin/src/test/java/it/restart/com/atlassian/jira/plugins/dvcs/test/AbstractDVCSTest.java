package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.rest.api.issue.IssueCreateResponse;
import com.atlassian.jira.testkit.client.Backdoor;
import com.atlassian.jira.testkit.client.restclient.Issue;
import com.atlassian.jira.testkit.client.restclient.SearchRequest;
import com.atlassian.jira.testkit.client.restclient.SearchResult;
import com.atlassian.jira.testkit.client.util.TestKitLocalEnvironmentData;
import com.atlassian.pageobjects.TestedProductFactory;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Abstract test for all DVCS tests.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public abstract class AbstractDVCSTest
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

    protected Backdoor testKit;

    /**
     * Prepares common test environment.
     */
    @BeforeClass
    public void onTestsEnvironmentSetup()
    {
        testKit = new Backdoor(new TestKitLocalEnvironmentData(new Properties(),"."));
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
        final IssueCreateResponse issue = testKit.issues().createIssue(projectKey, issueSummary, JiraLoginPage.USER_ADMIN);

        String result = issue.key();

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
        final SearchResult search = testKit.search().getSearch(new SearchRequest().jql("summary ~ \"" + issueSummary + "\""));
        for (Issue issue : search.issues)
        {
            testKit.issues().deleteIssue(issue.key, true);
        }

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
