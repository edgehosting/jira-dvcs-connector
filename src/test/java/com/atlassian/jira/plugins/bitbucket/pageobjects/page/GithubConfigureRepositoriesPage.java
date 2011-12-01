package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Options;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import junit.framework.Assert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.openqa.selenium.By;

/**
 * Represents the page to link repositories to projects
 */
public class GithubConfigureRepositoriesPage extends BaseConfigureRepositoriesPage
{
    @ElementBy(id = "login_field")
    PageElement githubWebLoginField;

    @ElementBy(id = "password")
    PageElement githubWebPasswordField;

    @ElementBy(name = "commit")
    PageElement githubWebSubmitButton;

    /**
     * Links a public repository to the given JIRA project
     *
     * @param projectKey    The JIRA project key
     * @param url           The url to the bitucket public repo
     * @param adminUsername username used to install the service (postcommit hook)
     * @param adminPassword password used to install the service (postcommit hook)
     * @return BitBucketConfigureRepositoriesPage
     */
    @Override
    public String addPublicRepoToProjectAndInstallService(String projectKey, String url, String adminUsername, String adminPassword)
    {
        urlTextbox.clear().type(url);
        projectSelect.select(Options.value(projectKey));
        addRepositoryButton.click();
        Poller.waitUntil(addedRepositoryH2.timed().getText(), IsEqual.equalTo("New Github repository"), Poller.by(15000));
        // postcommit hook
        addPostCommitServiceCheckbox.click();
        // add
        addRepositoryButton.click();

        String githubWebLoginRedirectUrl = checkAndDoGithubLogin();

        if (jiraTestedProduct.getTester().getDriver().elementExists(By.name("authorize")))
        {
            jiraTestedProduct.getTester().getDriver().findElement(By.name("authorize")).click();
        }

        if (!jiraTestedProduct.getTester().getDriver().getCurrentUrl().startsWith(jiraTestedProduct.getProductInstance().getBaseUrl()))
        {
            Assert.fail("Expected was Valid OAuth login and redirect to jira!");
        }


        Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isVisible());
        Poller.waitUntilTrue("Expected sync status message to be 'Sync Finished'", syncStatusDiv.find(By.tagName("strong")).timed()
                .hasText("Sync Finished:"));

        return addedRepositoryIdSpan.timed().getValue().byDefaultTimeout();
    }

    /**
     * Links a public repository to the given JIRA project
     *
     * @param projectKey The JIRA project key
     * @param url        The url to the bitucket public repo
     * @return BitBucketConfigureRepositoriesPage
     */
    @Override
    public BaseConfigureRepositoriesPage addPublicRepoToProjectSuccessfully(String projectKey, String url)
    {
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();

        Poller.waitUntil(addedRepositoryH2.timed().getText(), IsEqual.equalTo("New Github repository"), Poller.by(10000));
        addRepositoryButton.click();

        Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isVisible());
        Poller.waitUntil("Expected sync status message to be 'Sync Finished'", syncStatusDiv.find(By.tagName("strong")).timed()
                .getText(), Matchers.startsWith("Sync Finished:"), Poller.by(15000));

        return this;
    }

    /**
     * Links a public repository to the given JIRA project
     *
     * @param projectKey The JIRA project key
     * @param url        The url to the bitucket public repo
     * @return BitBucketConfigureRepositoriesPage
     */
    @Override
    public GithubConfigureRepositoriesPage addRepoToProjectFailingStep1(String projectKey, String url)
    {
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();

        Poller.waitUntilTrue("Expected Error message while connecting repository", messageBarDiv.find(By.tagName("strong")).timed()
                .hasText("Error!"));
        return this;
    }

    @Override
    public BaseConfigureRepositoriesPage addRepoToProjectFailingStep2(String projectKey, String url)
    {
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();

        Poller.waitUntil(addedRepositoryH2.timed().getText(), IsEqual.equalTo("New Github repository"), Poller.by(10000));
        addRepositoryButton.click();

        String githubWebLoginRedirectUrl = checkAndDoGithubLogin();
        String failedAuthorizationActionUrl = "https://github.com/login/oauth/authorize";
        if (!githubWebLoginRedirectUrl.startsWith(failedAuthorizationActionUrl))
        {
            Assert.fail("Expected was InValid OAuth configuration!");
        }

        return this;
    }

    private String checkAndDoGithubLogin()
    {
        githubWebLoginField.type("jirabitbucketconnector");
        githubWebPasswordField.type("jirabitbucketconnector1");
        githubWebSubmitButton.click();
        return jiraTestedProduct.getTester().getDriver().getCurrentUrl();
    }

    /**
     * Links a public repository to the given JIRA project
     *
     * @param projectKey The JIRA project key
     * @param url        The url to the bitucket public repo
     * @return BitBucketConfigureRepositoriesPage
     */
    @Override
    public GithubConfigureRepositoriesPage addPrivateRepoToProjectSuccessfully(String projectKey, String url)
    {
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();
        Poller.waitUntil(addedRepositoryH2.timed().getText(), IsEqual.equalTo("New Github repository"), Poller.by(10000));
        if (messageBarDiv.isPresent())
        {
            PageElement messageBarErrorDiv = messageBarDiv.find(By.className("error"));
            if (messageBarErrorDiv.isPresent())
            {
                Assert.fail("We not expected OAuth problem - OAuth should be configured successfully before adding repo in this test!");
            }
        }
        addRepositoryButton.click();
        String currentUrl = jiraTestedProduct.getTester().getDriver().getCurrentUrl();
        final String githubLoginUrl = "https://github.com/login";
        Assert.assertTrue("Expected redirect to github.com to login", currentUrl.startsWith(githubLoginUrl));

        githubWebLoginField.type("jirabitbucketconnector");
        githubWebPasswordField.type("jirabitbucketconnector1");
        githubWebSubmitButton.click();

        if (jiraTestedProduct.getTester().getDriver().elementExists(By.name("authorize")))
        {
            jiraTestedProduct.getTester().getDriver().findElement(By.name("authorize")).click();
        }

        currentUrl = jiraTestedProduct.getTester().getDriver().getCurrentUrl();
        if (!currentUrl.startsWith(jiraTestedProduct.getProductInstance().getBaseUrl()))
        {
            Assert.fail("Expected was automatic continue to jira!");
        }
        Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isVisible());
        Poller.waitUntil("Expected sync status message to be 'Sync Finished'", syncStatusDiv.find(By.tagName("strong")).timed()
                .getText(), Matchers.startsWith("Sync Finished:"), Poller.by(15000));
        return this;
    }
}
