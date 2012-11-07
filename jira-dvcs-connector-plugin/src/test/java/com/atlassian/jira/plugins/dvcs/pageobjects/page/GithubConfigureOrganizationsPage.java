package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import static org.hamcrest.Matchers.containsString;

import java.util.List;

import junit.framework.Assert;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

/**
 * Represents the page to link repositories to projects
 */
public class GithubConfigureOrganizationsPage extends BaseConfigureOrganizationsPage
{

    @ElementBy(id = "login_field")
    PageElement githubWebLoginField;

    @ElementBy(id = "password")
    PageElement githubWebPasswordField;

    @ElementBy(name = "authorize")
    PageElement githubWebAuthorizeButton;

    @ElementBy(name = "commit")
    PageElement githubWebSubmitButton;

    @ElementBy(id = "oauthClientId")
    PageElement clientId;

    @ElementBy(id = "oauthSecret")
    PageElement secretId;

    @Override
    public GithubConfigureOrganizationsPage addOrganizationSuccessfully(String url, String organizationAccount, boolean autoSync)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(1));

        organization.clear().type(organizationAccount);

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
    public GithubConfigureOrganizationsPage addOrganizationFailingStep1(String url)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(1));

        organization.clear().type(url);

        setPageAsOld();

        addOrgButton.click();

        Poller.waitUntilTrue("Expected Error message while connecting repository", messageBarDiv.find(By.tagName("strong")).timed()
                .hasText("Error!"));

        return this;
    }

    @Override
    public BaseConfigureOrganizationsPage addRepoToProjectFailingStep2(String url)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(1));
        organization.clear().type("jirabitbucketconnector");

        setPageAsOld();

        addOrgButton.click();

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
    public BaseConfigureOrganizationsPage addRepoToProjectFailingPostcommitService(String url)
    {
        addRepoToProject(url, true);

        assertThatErrorMessage(containsString("Error adding postcommit hook. Do you have admin rights to the repository?\n" +
                "Repository was not added. [Could not add postcommit hook. ]"));

        return this;
    }


    public GithubConfigureOrganizationsPage addRepoToProject(String url, boolean autoSync)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(1));
        organization.clear().type("jirabitbucketconnector");

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

    public GithubConfigureOrganizationsPage addRepoToProjectForOrganization(String organizationString)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(1));
        organization.clear().type(organizationString);

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

    public int getNumberOfVisibleRepositories()
    {
        List<WebElement> visibleRepositoryRows = jiraTestedProduct.getTester()
                                                                  .getDriver()
                                                                  .findElements(By.className("dvcs-repo-row"));

        return visibleRepositoryRows.size();
    }
}
