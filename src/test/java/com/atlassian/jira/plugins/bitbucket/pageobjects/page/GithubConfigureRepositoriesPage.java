package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Options;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import junit.framework.Assert;
import org.openqa.selenium.By;

import static org.hamcrest.Matchers.containsString;

/**
 * Represents the page to link repositories to projects
 */
public class GithubConfigureRepositoriesPage extends BaseConfigureRepositoriesPage
{
    @ElementBy(id = "clientID")
    PageElement ghClientID;

    @ElementBy(id = "clientSecret")
    PageElement ghClientSecret;

    @ElementBy(id = "gh_messages")
    PageElement ghMessagesDiv;

    @ElementBy(id = "login_field")
    PageElement githubWebLoginField;

    @ElementBy(id = "password")
    PageElement githubWebPasswordField;

    @ElementBy(name = "authorize")
    PageElement githubWebAuthorizeButton;

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
        linkRepositoryButton.click();
        waitFormBecomeVisible();
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();

        checkAndDoGithubLogin();
        String githubWebLoginRedirectUrl = authorizeGithubAppIfRequired();
        if (!githubWebLoginRedirectUrl.contains("/jira/"))
        {
            Assert.fail("Expected was Valid OAuth login and redirect to JIRA!");
        }
        return addedRepositoryIdSpan.timed().getValue().now();
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
        addRepoToProject(projectKey, url);
        checkSyncProcessSuccess();
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
        linkRepositoryButton.click();
        waitFormBecomeVisible();
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
        linkRepositoryButton.click();
        waitFormBecomeVisible();
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();

        String currentUrl = checkAndDoGithubLogin();
        String expectedUrl = "https://github.com/login/oauth/authorize?";
        if (!currentUrl.startsWith(expectedUrl) || !currentUrl.contains("client_id=xxx"))
        {
            Assert.fail("Unexpected url: " + currentUrl);
        }

        return this;
    }

    private String checkAndDoGithubLogin()
    {
        waitWhilePageLaoded();
        String currentUrl = jiraTestedProduct.getTester().getDriver().getCurrentUrl();
        if (currentUrl.contains("https://github.com/login?"))
        {
            githubWebLoginField.type("jirabitbucketconnector");
            githubWebPasswordField.type("jirabitbucketconnector1");
            githubWebSubmitButton.click();
        }
        return jiraTestedProduct.getTester().getDriver().getCurrentUrl();
    }

    private void waitWhilePageLaoded()
    {
        try
        {
            Thread.sleep(5000);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private String authorizeGithubAppIfRequired()
    {
        waitWhilePageLaoded();
        String currentUrl = jiraTestedProduct.getTester().getDriver().getCurrentUrl();
        if (currentUrl.contains("/github.com/login/oauth"))
        {
            githubWebAuthorizeButton.click();
        }
        return jiraTestedProduct.getTester().getDriver().getCurrentUrl();
    }

    @Override
    public BaseConfigureRepositoriesPage addRepoToProjectFailingPostcommitService(String projectKey, String url)
    {
        addRepoToProject(projectKey, url);
        assertThatSuccessMessage(containsString("Repository added"));
        assertThatWarningMessage(containsString("Error adding postcommit service."));
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
    public GithubConfigureRepositoriesPage addPrivateRepoToProjectSuccessfully(String projectKey, String url)
    {
        addRepoToProject(projectKey, url);
        checkSyncProcessSuccess();
        return this;
    }

    public GithubConfigureRepositoriesPage addRepoToProject(String projectKey, String url)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();

        String currentUrl = checkAndDoGithubLogin();
        currentUrl = authorizeGithubAppIfRequired();
        if (!currentUrl.contains("/jira/"))
        {
            Assert.fail("Expected was automatic continue to jira!");
        }

        return this;
    }
}
