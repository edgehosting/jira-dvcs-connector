package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Options;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;

/**
 * Represents the page to link repositories to projects
 */
public class BitBucketConfigureRepositoriesPage extends BaseConfigureRepositoriesPage
{

    @ElementBy(id = "adminUsername")
    PageElement adminUsernameInput;

    @ElementBy(id = "adminPassword")
    PageElement adminPasswordInput;


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
        addRepoToProjectSuccessfully(projectKey, url);
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
    public BitBucketConfigureRepositoriesPage addRepoToProjectFailingStep1(String projectKey, String url)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();
        Poller.waitUntilTrue("Expected Error message while connecting repository", messageBarDiv.find(By.tagName("strong")).timed().hasText("Error!"));
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
        Poller.waitUntilTrue("Expected form for bitbucket repository admin login/password!", Conditions.and(adminUsernameInput.timed().isVisible(), adminPasswordInput.timed().isVisible()));
        return this;
    }

    @Override
    public BaseConfigureRepositoriesPage addRepoToProjectFailingPostcommitService(String projectKey, String url)
    {
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
    public BitBucketConfigureRepositoriesPage addRepoToProjectSuccessfully(String projectKey, String url)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();
        Poller.waitUntilTrue(adminUsernameInput.timed().isVisible());
        adminUsernameInput.type("jirabitbucketconnector");
        adminPasswordInput.type("jirabitbucketconnector1");
        addRepositoryButton.click();

        checkSyncProcessSuccess();

        return this;
    }
}
