package it.com.atlassian.jira.plugins.dvcs;

import java.util.List;

import org.openqa.selenium.By;

import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BaseConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubOAuthConfigPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.pages.JiraLoginPage;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for BitBucket integration tests. Initializes the JiraTestedProduct and logs admin in.
 */
public abstract class BaseOrganizationTest<T extends BaseConfigureOrganizationsPage>
{
    protected static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    protected T configureOrganizations;

    public static class AnotherLoginPage extends JiraLoginPage
    {
        @Override
        public void doWait()
        {
            // hacking of confirmation dialog: "Are you sure you want to navigate away from this page?"
            try
            {
                String tagName = driver.switchTo().activeElement().getTagName();
                String inputType = driver.switchTo().activeElement().getAttribute("type");
                if ("input".equals(tagName) && "button".equals(inputType))
                {
                    driver.switchTo().alert().accept();
                }
            } catch (Exception e)
            {
                // ignore if no alert shown
            }
            // waiting for login page
            driver.waitUntilElementIsLocated(By.name("os_username"));
        }
    }

    @BeforeMethod
    public void loginToJira()
    {
        jira.getPageBinder().navigateToAndBind(AnotherLoginPage.class).loginAsSysAdmin(getConfigureOrganizationsPageClass());
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

    protected GithubOAuthConfigPage goToGithubOAuthConfigPage()
    {
        return jira.visit(GithubOAuthConfigPage.class);
    }

    protected BaseConfigureOrganizationsPage goToConfigPage()
    {
        configureOrganizations = jira.visit(getConfigureOrganizationsPageClass());
        configureOrganizations.setJiraTestedProduct(jira);
        return configureOrganizations;
    }
}
