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
        setPageAsOld();
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
    public GithubConfigureRepositoriesPage addRepoToProjectFailingStep1(String projectKey, String url)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        setPageAsOld();
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
        setPageAsOld();
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
        waitWhileNewPageLaoded();
        String currentUrl = jiraTestedProduct.getTester().getDriver().getCurrentUrl();
        if (currentUrl.contains("https://github.com/login?"))
        {
            githubWebLoginField.type("jirabitbucketconnector");
            githubWebPasswordField.type("jirabitbucketconnector1");
            setPageAsOld();
            githubWebSubmitButton.click();
        }
        return jiraTestedProduct.getTester().getDriver().getCurrentUrl();
    }

    private void waitWhileNewPageLaoded()
    {
        jiraTestedProduct.getTester().getDriver().waitUntilElementIsNotLocated(By.id("old-page"));
    }

    protected void setPageAsOld()
    {
        StringBuilder script = new StringBuilder();
        script.append("var bodyElm = document.getElementsByTagName('body')[0];");
        script.append("var oldPageHiddenElm = document.createElement('input');");
        script.append("oldPageHiddenElm.setAttribute('id','old-page');");
        script.append("oldPageHiddenElm.setAttribute('type','hidden');");
        script.append("bodyElm.appendChild(oldPageHiddenElm);");
        jiraTestedProduct.getTester().getDriver().executeScript(script.toString());
    }

    private String authorizeGithubAppIfRequired()
    {
        waitWhileNewPageLaoded();
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
        assertThatErrorMessage(containsString("Error adding postcommit hook. Do you have admin rights to the repository?\n" +
                "Repository was not added. [Could not add postcommit hook. ]"));
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
    public GithubConfigureRepositoriesPage addRepoToProjectSuccessfully(String projectKey, String url)
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
        setPageAsOld();
        addRepositoryButton.click();

        checkAndDoGithubLogin();
        String currentUrl = authorizeGithubAppIfRequired();
        if (!currentUrl.contains("/jira/"))
        {
            Assert.fail("Expected was automatic continue to jira!");
        }

        return this;
    }
}
