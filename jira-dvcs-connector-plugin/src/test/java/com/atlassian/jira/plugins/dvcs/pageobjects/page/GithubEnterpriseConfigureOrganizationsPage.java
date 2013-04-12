package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import it.githubenterprise.com.atlassian.jira.plugins.dvcs.GithubEnterpriseOrganizationsTest;

import org.openqa.selenium.By;
import org.testng.Assert;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

/**
 * Represents the page to link repositories to projects
 */
public class GithubEnterpriseConfigureOrganizationsPage extends GithubConfigureOrganizationsPage
{
    @ElementBy(xpath="//button[contains(concat(' ', @class , ' '),' button-panel-submit-button ') and text()='Continue']")
    PageElement continueAddOrgButton;

    @Override
    public GithubEnterpriseConfigureOrganizationsPage addOrganizationSuccessfully(String organizationAccount, boolean autoSync)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(2));

        organization.clear().type(organizationAccount);

        setPageAsOld();

        if (!autoSync) {
            autoLinkNewRepos.click();
        }

        addOrgButton.click();

        // Confirm submit for GitHub Enterprise
        continueAddOrgButton.click();
        
        checkAndDoGithubLogin();

        String githubWebLoginRedirectUrl = authorizeGithubAppIfRequired();

        if (!githubWebLoginRedirectUrl.contains("jira"))
        {
            Assert.fail("Expected was Valid OAuth login and redirect to JIRA!");
        }

        if (autoSync) {
            JiraPageUtils.checkSyncProcessSuccess(jiraTestedProduct);
        }

        return this;
    }

    @Override
    public GithubEnterpriseConfigureOrganizationsPage addOrganizationFailingStep1(String url)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(2));

        organization.clear().type(url);

        setPageAsOld();

        addOrgButton.click();

        // Confirm submit for GitHub Enterprise
        continueAddOrgButton.click();
        
        Poller.waitUntilTrue("Expected Error message while connecting repository", messageBarDiv.find(By.tagName("strong")).timed()
                .hasText("Error!"));

        return this;
    }

    @Override
    public BaseConfigureOrganizationsPage addRepoToProjectFailingStep2()
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(2));
        organization.clear().type("jirabitbucketconnector");
        
        setPageAsOld();

        addOrgButton.click();

        // Confirm submit for GitHub Enterprise
        continueAddOrgButton.click();
        
        String currentUrl = checkAndDoGithubLogin();
        String expectedUrl = GithubEnterpriseOrganizationsTest.GITHUB_ENTERPRISE_URL + "/login/oauth/authorize?";
        if (!currentUrl.startsWith(expectedUrl) || !currentUrl.contains("client_id=xxx"))
        {
            Assert.fail("Unexpected url: " + currentUrl);
        }

        return this;
    }

    private String checkAndDoGithubLogin()
    {

        waitWhileNewPageLaoded();

// We don't need to login
//        String currentUrl = jiraTestedProduct.getTester().getDriver().getCurrentUrl();
//        if (currentUrl.contains(GithubEnterpriseOrganizationsTest.GITHUB_ENTERPRISE_URL + "/login"))
//        {
//            githubWebLoginField.type("jirabitbucketconnector");
//            githubWebPasswordField.type(PasswordUtil.getPassword("jirabitbucketconnector"));
//            setPageAsOld();
//            githubWebSubmitButton.click();
//        }
        return jiraTestedProduct.getTester().getDriver().getCurrentUrl();
    }

    private String authorizeGithubAppIfRequired()
    {
        waitWhileNewPageLaoded();

        String currentUrl = jiraTestedProduct.getTester().getDriver().getCurrentUrl();
        if (currentUrl.contains(GithubEnterpriseOrganizationsTest.GITHUB_ENTERPRISE_URL + "/login/oauth"))
        {
            githubWebAuthorizeButton.click();
        }
        return jiraTestedProduct.getTester().getDriver().getCurrentUrl();
    }

    @Override
    public GithubEnterpriseConfigureOrganizationsPage addRepoToProject(String url, boolean autoSync)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(2));
        organization.clear().type("jirabitbucketconnector");
        
        setPageAsOld();
        addOrgButton.click();
     
        // Confirm submit for GitHub Enterprise
        continueAddOrgButton.click();
        
        checkAndDoGithubLogin();
        String currentUrl = authorizeGithubAppIfRequired();
        if (!currentUrl.contains("jira"))
        {
            Assert.fail("Expected was automatic continue to jira!");
        }

        return this;
    }

    @Override
    public GithubEnterpriseConfigureOrganizationsPage addRepoToProjectForOrganization(String organizationString)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(2));
        organization.clear().type(organizationString);

        setPageAsOld();
        addOrgButton.click();

        // Confirm submit for GitHub Enterprise
        continueAddOrgButton.click();
        
        checkAndDoGithubLogin();
        String currentUrl = authorizeGithubAppIfRequired();
        if (!currentUrl.contains("/jira/"))
        {
            Assert.fail("Expected was automatic continue to jira!");
        }

        return this;
    }
}
