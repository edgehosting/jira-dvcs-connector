package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Options;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.IsEqual;
import org.openqa.selenium.By;

/**
 * Represents the page to link repositories to projects
 */
public class GithubConfigureRepositoriesPage extends BaseConfigureRepositoriesPage
{
    @ElementBy(id = "ghUsername")
    PageElement bbUsernameInput;

    @ElementBy(id = "ghPassword")
    PageElement bbPasswordInput;


    /**
     * Links a public repository to the given JIRA project
     *
     * @param projectKey    The JIRA project key
     * @param url           The url to the bitucket public repo
     * @param adminUsername username used to install the service (postcommit hook)
     * @param adminPassword password used to install the service (postcommit hook)
     * @return BitBucketConfigureRepositoriesPage
     */
    public String addPublicRepoToProjectAndInstallService(String projectKey, String url, String adminUsername,
                                                          String adminPassword)
    {
        urlTextbox.clear().type(url);
        projectSelect.select(Options.value(projectKey));
        addRepositoryButton.click();
        Poller.waitUntil(addedRepositoryH2.timed().getText(), AnyOf.anyOf(new IsEqual<String>("New Bitbucket repository"), new IsEqual<String>("New Github repository")));
        // postcommit hook
        addPostCommitServiceCheckbox.click();
        adminUsernameTextbox.clear().type(adminUsername);
        adminPasswordTextbox.clear().type(adminPassword);
        // add
        addRepositoryButton.click();

        // TODO: remove following line and uncomment next 2 lines after GUI fix of showing sync_message div during synchronisation
        Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isPresent());
        //Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isVisible());
        //Poller.waitUntilTrue("Expected sync status message to be 'Sync Finished'", syncStatusDiv.find(By.tagName("strong")).timed().hasText("Sync Finished:"));

        return addedRepositoryIdSpan.timed().getValue().byDefaultTimeout();
    }

    /**
     * Links a public repository to the given JIRA project
     *
     * @param projectKey The JIRA project key
     * @param url        The url to the bitucket public repo
     * @return BitBucketConfigureRepositoriesPage
     */
    public BaseConfigureRepositoriesPage addPublicRepoToProjectSuccessfully(String projectKey, String url)
    {
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();

        // TODO: remove following line and uncomment next 2 lines after GUI fix of showing sync_message div during synchronisation
        Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isPresent());
        //Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isVisible());
        //Poller.waitUntilTrue("Expected sync status message to be 'Sync Finished'", syncStatusDiv.find(By.tagName("strong")).timed().hasText("Sync Finished:"));

        return this;
    }

    /**
     * Links a public repository to the given JIRA project
     *
     * @param projectKey The JIRA project key
     * @param url        The url to the bitucket public repo
     * @return BitBucketConfigureRepositoriesPage
     */
    public GithubConfigureRepositoriesPage addRepoToProjectFailing(String projectKey, String url)
    {
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();
        Poller.waitUntilTrue("Expected Error message while connecting repository", messageBarDiv.find(By.tagName("strong")).timed().hasText("Error!"));
        return this;
    }

    /**
     * Links a public repository to the given JIRA project
     *
     * @param projectKey The JIRA project key
     * @param url        The url to the bitucket public repo
     * @return BitBucketConfigureRepositoriesPage
     */
    public GithubConfigureRepositoriesPage addPrivateRepoToProjectSuccessfully(String projectKey, String url)
    {
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();
        Poller.waitUntil(addedRepositoryH2.timed().getText(), AnyOf.anyOf(new IsEqual<String>("New Bitbucket repository"), new IsEqual<String>("New Github repository")));
        bbUsernameInput.type("jirabitbucketconnector");
        bbPasswordInput.type("jirabitbucketconnector1");
        addRepositoryButton.click();

        // TODO: remove following line and uncomment next 2 lines after GUI fix of showing sync_message div during synchronisation
        Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isPresent());
        //Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isVisible());
        //Poller.waitUntilTrue("Expected sync status message to be 'Sync Finished'", syncStatusDiv.find(By.tagName("strong")).timed().hasText("Sync Finished:"));

        return this;
    }
}
