package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import static org.hamcrest.Matchers.containsString;

import it.com.atlassian.jira.plugins.dvcs.GithubEnterpriseOrganizationsTest;

import org.openqa.selenium.By;

import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

import org.testng.Assert;

/**
 * Represents the page to link repositories to projects
 */
public class GithubEnterpriseConfigureOrganizationsPage extends GithubConfigureOrganizationsPage
{
    @ElementBy(name = "urlGhe")
    PageElement githubEnterpriseHostUrl;
    
    private String githubEnterpriseHost;//TODO ???
    
    public void setGithubEnterpriseHost(String githubEnterpriseHost)
    {
        this.githubEnterpriseHost = githubEnterpriseHost;
    }
    
    @Override
    public GithubEnterpriseConfigureOrganizationsPage addOrganizationSuccessfully(String organizationAccount, boolean autoSync)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(2));

        organization.clear().type(organizationAccount);

        githubEnterpriseHostUrl.clear().type(GithubEnterpriseOrganizationsTest.GITHUB_ENTERPRISE_URL);
        
        setPageAsOld();

        if (!autoSync) {
        	autoLinkNewRepos.click();
        }

        addOrgButton.click();

        checkAndDoGithubLogin();

        String githubWebLoginRedirectUrl = authorizeGithubAppIfRequired();

        if (!githubWebLoginRedirectUrl.contains("/jira/"))
        {
            Assert.fail("Expected was Valid OAuth login and redirect to JIRA!");
        }

        if (autoSync) {
        	checkSyncProcessSuccess();
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

        githubEnterpriseHostUrl.clear().type(GithubEnterpriseOrganizationsTest.GITHUB_ENTERPRISE_URL);
        
        setPageAsOld();

        addOrgButton.click();

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
        githubEnterpriseHostUrl.clear().type(GithubEnterpriseOrganizationsTest.GITHUB_ENTERPRISE_URL);
        
        setPageAsOld();

        addOrgButton.click();

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

        String currentUrl = jiraTestedProduct.getTester().getDriver().getCurrentUrl();
        if (currentUrl.contains(GithubEnterpriseOrganizationsTest.GITHUB_ENTERPRISE_URL + "/login?"))
        {
            githubWebLoginField.type("jirabitbucketconnector");
            githubWebPasswordField.type(PasswordUtil.getPassword("jirabitbucketconnector"));
            setPageAsOld();
            githubWebSubmitButton.click();
        }
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
    public BaseConfigureOrganizationsPage addRepoToProjectFailingPostcommitService(String url)
    {
        addRepoToProject(url, true);

        assertThatErrorMessage(containsString("Error adding postcommit hook. Do you have admin rights to the repository?\n" +
                "Repository was not added. [Could not add postcommit hook. ]"));

        return this;
    }

    @Override
    public GithubEnterpriseConfigureOrganizationsPage addRepoToProject(String url, boolean autoSync)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(2));
        organization.clear().type("jirabitbucketconnector");
        githubEnterpriseHostUrl.clear().type(GithubEnterpriseOrganizationsTest.GITHUB_ENTERPRISE_URL);
        
        setPageAsOld();
        addOrgButton.click();

        checkAndDoGithubLogin();
        String currentUrl = authorizeGithubAppIfRequired();
        if (!currentUrl.contains("/jira/"))
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
        githubEnterpriseHostUrl.clear().type(GithubEnterpriseOrganizationsTest.GITHUB_ENTERPRISE_URL);

        setPageAsOld();
        addOrgButton.click();

        checkAndDoGithubLogin();
        String currentUrl = authorizeGithubAppIfRequired();
        if (!currentUrl.contains("/jira/"))
        {
            Assert.fail("Expected was automatic continue to jira!");
        }

        return this;
    }
}
