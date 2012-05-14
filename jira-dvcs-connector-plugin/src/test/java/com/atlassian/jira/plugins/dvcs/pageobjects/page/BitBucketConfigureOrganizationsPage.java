package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Options;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;

/**
 * Represents the page to link repositories to projects.
 */
public class BitBucketConfigureOrganizationsPage extends BaseConfigureOrganizationsPage
{

    /** The admin username input. */
    @ElementBy(id = "adminUsername")
    PageElement adminUsernameInput;

    /** The admin password input. */
    @ElementBy(id = "adminPassword")
    PageElement adminPasswordInput;


    /**
     * Links a public repository to the given JIRA project.
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
    	addOrganizationSuccessfully( url);
        return addedRepositoryIdSpan.timed().getValue().byDefaultTimeout();
    }


    /**
     * Links a public repository to the given JIRA project.
     *
     * @param projectKey The JIRA project key
     * @param url        The url to the bitucket public repo
     * @return BitBucketConfigureRepositoriesPage
     */
    @Override
    public BitBucketConfigureOrganizationsPage addRepoToProjectFailingStep1(String projectKey, String url)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();
        Poller.waitUntilTrue("Expected Error message while connecting repository", messageBarDiv.find(By.tagName("strong")).timed().hasText("Error!"));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseConfigureOrganizationsPage addRepoToProjectFailingStep2(String projectKey, String url)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();
        projectSelect.select(Options.value(projectKey));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();
        Poller.waitUntilTrue("Expected form for bitbucket repository admin login/password!", Conditions.and(adminUsernameInput.timed().isVisible(), adminPasswordInput.timed().isVisible()));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseConfigureOrganizationsPage addRepoToProjectFailingPostcommitService(String projectKey, String url)
    {
        return this;
    }

    /**
     * Links a public repository to the given JIRA project.
     *
     * @param projectKey The JIRA project key
     * @param url        The url to the bitucket public repo
     * @return BitBucketConfigureRepositoriesPage
     */


    @Override
    public BitBucketConfigureOrganizationsPage addOrganizationSuccessfully(String url)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();
        
        urlTextbox.clear().type(url);
        organization.clear().type("jirabitbucketconnector");
        addRepositoryButton.click();
        
        Poller.waitUntilTrue(adminUsernameInput.timed().isVisible());
        
        adminUsernameInput.type("jirabitbucketconnector");
        adminPasswordInput.type("jirabitbucketconnector1");
        addRepositoryButton.click();

        checkSyncProcessSuccess();

        return this;
    }
}
