package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.ElementBy;
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


    @Override
    public BitBucketConfigureOrganizationsPage addOrganizationSuccessfully(String url, boolean autoSync)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();
        
        urlTextbox.clear().type(url);
        organization.clear().type("jirabitbucketconnector");
        addOrgButton.click();
        
        if (autoSync) {
        	autoLinkNewRepos.click();
        }
        
        Poller.waitUntilTrue(adminUsernameInput.timed().isVisible());
        
        adminUsernameInput.type("jirabitbucketconnector");
        adminPasswordInput.type("jirabitbucketconnector1");
        addOrgButton.click();

        if (autoSync) {
        	checkSyncProcessSuccess();
        }

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
    public BitBucketConfigureOrganizationsPage addRepoToProjectFailingStep1(String projectKey, String url)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();
        
        urlTextbox.clear().type(url);
        organization.clear().type("jirabitbucketconnector");

        addOrgButton.click();

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
        urlTextbox.clear().type(url);
        addOrgButton.click();
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


}
