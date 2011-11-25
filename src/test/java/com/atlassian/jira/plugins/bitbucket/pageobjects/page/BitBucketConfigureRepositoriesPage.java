package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import org.hamcrest.core.AnyOf;
import org.hamcrest.core.IsEqual;
import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Options;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

/**
 * Represents the page to link repositories to projects
 */
public class BitBucketConfigureRepositoriesPage extends BaseConfigureRepositoriesPage
{
    @ElementBy(id = "bbUsername")
    PageElement bbUsernameInput;

    @ElementBy(id = "bbPassword")
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
    @Override
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

        Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isVisible());
        Poller.waitUntilTrue("Expected sync status message to be 'Sync Finished'", syncStatusDiv.find(By.tagName("strong")).timed().hasText("Sync Finished:"));

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
        Poller.waitUntil(addedRepositoryH2.timed().getText(), AnyOf.anyOf(new IsEqual<String>("New Bitbucket repository"), new IsEqual<String>("New Github repository")));
        addRepositoryButton.click();

        Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isVisible());
        Poller.waitUntilTrue("Expected sync status message to be 'Sync Finished'", syncStatusDiv.find(By.tagName("strong")).timed().hasText("Sync Finished:"));

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
    public BitBucketConfigureRepositoriesPage addRepoToProjectFailing(String projectKey, String url)
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
    @Override
    public BitBucketConfigureRepositoriesPage addPrivateRepoToProjectSuccessfully(String projectKey, String url)
    {
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();
        Poller.waitUntil(addedRepositoryH2.timed().getText(), AnyOf.anyOf(new IsEqual<String>("New Bitbucket repository"), new IsEqual<String>("New Github repository")));
        bbUsernameInput.type("jirabitbucketconnector");
        bbPasswordInput.type("jirabitbucketconnector");
        addRepositoryButton.click();

        Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isVisible());
        Poller.waitUntilTrue("Expected sync status message to be 'Sync Finished'", syncStatusDiv.find(By.tagName("strong")).timed().hasText("Sync Finished:"));

        return this;
    }
}
