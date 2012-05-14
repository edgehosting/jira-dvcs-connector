package it.com.atlassian.jira.plugins.dvcs;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.By;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.GithubOAuthConfigPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.JiraViewIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BaseConfigureOrganizationsPage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import com.atlassian.webdriver.jira.page.JiraLoginPage;

/**
 * Base class for BitBucket integration tests. Initializes the JiraTestedProduct and logs admin in.
 */
public abstract class BitBucketBaseOrgTest
{
    protected static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    protected BaseConfigureOrganizationsPage configureOrganizations;

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

    @SuppressWarnings("unchecked")
    @Before
    public void loginToJira()
    {
        configureOrganizations = (BaseConfigureOrganizationsPage) jira.getPageBinder().navigateToAndBind(AnotherLoginPage.class).loginAsSysAdmin(getPageClass());
        configureOrganizations.setJiraTestedProduct(jira);
        configureOrganizations.deleteAllOrganizations();
    }

    @SuppressWarnings("rawtypes")
    protected abstract Class getPageClass();

    @After
    public void logout()
    {
        jira.getTester().getDriver().manage().deleteAllCookies();
    }

    protected void ensureOrganizationPresent(String projectKey, String repoUrl)
    {
    /*    if (!configureOrganizations.isRepositoryPresent(projectKey, repoUrl))
        {
            configureOrganizations.addOrganizationSuccessfully(repoUrl);
        }*/
    }


    protected List<BitBucketCommitEntry> getCommitsForIssue(String issueKey)
    {
        return jira.visit(JiraViewIssuePage.class, issueKey)
                .openBitBucketPanel()
                .waitForMessages();
    }

    protected GithubOAuthConfigPage goToGithubOAuthConfigPage()
    {
        return jira.visit(GithubOAuthConfigPage.class);
    }

    protected BaseConfigureOrganizationsPage goToRepositoriesConfigPage()
    {
        configureOrganizations = (BaseConfigureOrganizationsPage) jira.visit(getPageClass());
        configureOrganizations.setJiraTestedProduct(jira);
        return configureOrganizations;
    }
}
