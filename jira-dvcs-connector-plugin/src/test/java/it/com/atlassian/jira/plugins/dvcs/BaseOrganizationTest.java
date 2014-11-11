package it.com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BaseConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.jira.plugins.dvcs.pageobjects.JiraLoginPageController;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.util.List;

/**
 * Base class for BitBucket integration tests. Initializes the JiraTestedProduct and logs admin in.
 */
public abstract class BaseOrganizationTest<T extends BaseConfigureOrganizationsPage> extends DvcsWebDriverTestCase
{
    protected static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    protected T configureOrganizations;

    @BeforeMethod
    public void loginToJira()
    {
        // log in to JIRA
        new JiraLoginPageController(jira).login(getConfigureOrganizationsPageClass());
        configureOrganizations = jira.getPageBinder().navigateToAndBind(getConfigureOrganizationsPageClass());
        configureOrganizations.setJiraTestedProduct(jira);
        configureOrganizations.deleteAllOrganizations();
    }

    protected abstract Class<T> getConfigureOrganizationsPageClass();

    @AfterMethod
    public void logout()
    {
        jira.getTester().getDriver().manage().deleteAllCookies();
    }


    protected List<BitBucketCommitEntry> getCommitsForIssue(String issueKey, int exectedNumberOfCommits)
    {
        return getCommitsForIssue(issueKey, exectedNumberOfCommits, 1000L, 5);
    }

    protected List<BitBucketCommitEntry> getCommitsForIssue(String issueKey, int exectedNumberOfCommits,
            long retryThreshold, int maxRetryCount)
    {
        return jira.visit(JiraViewIssuePage.class, issueKey)
                .openBitBucketPanel()
                .waitForNumberOfMessages(exectedNumberOfCommits, retryThreshold, maxRetryCount);
    }

    protected BaseConfigureOrganizationsPage goToConfigPage()
    {
        configureOrganizations = jira.visit(getConfigureOrganizationsPageClass());
        configureOrganizations.setJiraTestedProduct(jira);
        return configureOrganizations;
    }
}
