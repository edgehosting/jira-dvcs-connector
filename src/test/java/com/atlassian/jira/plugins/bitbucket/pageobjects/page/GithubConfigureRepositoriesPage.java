package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Options;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.IsEqual;
import org.openqa.selenium.By;

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
    public BaseConfigureRepositoriesPage addPublicRepoToProjectSuccessfully(String projectKey, String url)
    {
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
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
        if(messageBarDiv.isPresent()){
            PageElement messageBarErrorDiv = messageBarDiv.find(By.className("error"));
            if(messageBarErrorDiv.isPresent() && messageBarErrorDiv.timed().getText().now().contains("OAuth needs to be")){
                PageElement oauthLink = messageBarErrorDiv.find(By.linkText("configured before adding private github repository."));
                if(oauthLink.isPresent()){
                    String currentUrl = jiraTestedProduct.getTester().getDriver().getCurrentUrl();
                    jiraTestedProduct.getTester().getDriver().navigate().to(oauthLink.getAttribute("href"));
                    Poller.waitUntilTrue("Expected OAuth form to appear.", ghClientID.timed().isVisible());
                    ghClientID.type("281f79035707e14feebe");
                    ghClientSecret.type("2dd08e37c903782cc6e38a6a25b572887b6d5583");
                    addRepositoryButton.click();
                    Poller.waitUntilTrue("Expected sync status message to be 'GitHub Client Identifiers Set Correctly'", ghMessagesDiv.find(By.tagName("h2")).timed().hasText("GitHub Client Identifiers Set Correctly"));
                    jiraTestedProduct.getTester().getDriver().navigate().to(currentUrl);
                    Poller.waitUntil(addedRepositoryH2.timed().getText(), AnyOf.anyOf(new IsEqual<String>("New Bitbucket repository"), new IsEqual<String>("New Github repository")));

                }
            }
        }
        return this;
    }
}
