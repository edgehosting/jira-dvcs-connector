package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

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
    private PageElement oauthClientId;

    @ElementBy(id = "oauthSecret")
    private PageElement oauthSecret;


    @Override
    public GithubConfigureOrganizationsPage addOrganizationSuccessfully(String organizationAccount, OAuthCredentials oAuthCredentials, boolean autoSync, String username, String password)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(1));
        organization.clear().type(organizationAccount);

        oauthClientId.clear().type(oAuthCredentials.key);
        oauthSecret.clear().type(oAuthCredentials.secret);

        setPageAsOld();
        if (!autoSync)
        {
            autoLinkNewRepos.click();
        }

        addOrgButton.click();
        checkAndDoGithubLogin(username, password);

        String currentUrl = authorizeGithubAppIfRequired();
        if (!currentUrl.contains("jira"))
        {
            throw new AssertionError("Expected was Valid OAuth login and redirect to JIRA!");
        }

        Poller.waitUntilTrue(linkRepositoryButton.timed().isPresent());

        if (autoSync)
        {
            JiraPageUtils.checkSyncProcessSuccess(pageBinder);
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
    public BaseConfigureOrganizationsPage addOrganizationFailingOAuth(final String username, final String password)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(1));
        organization.clear().type("jirabitbucketconnector");

        oauthClientId.clear().type("xx");
        oauthSecret.clear().type("yy");


        setPageAsOld();

        addOrgButton.click();

        String currentUrl = checkAndDoGithubLogin(username, password);
        String expectedUrl = "https://github.com/login/oauth/authorize?";
        if (!currentUrl.startsWith(expectedUrl) || !currentUrl.contains("client_id=xxx"))
        {
            throw new AssertionError("Unexpected url: " + currentUrl);
        }

        return this;
    }

    private String checkAndDoGithubLogin(final String username, final String password)
    {

    	waitWhileNewPageLaoded();

        String currentUrl = jiraTestedProduct.getTester().getDriver().getCurrentUrl();
        if (currentUrl.contains("https://github.com/login?"))
        {
            githubWebLoginField.type(username);
            githubWebPasswordField.type(password);
            setPageAsOld();
            githubWebSubmitButton.click();
        }
        return jiraTestedProduct.getTester().getDriver().getCurrentUrl();
    }

    protected void waitWhileNewPageLaoded()
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



    public int getNumberOfVisibleRepositories()
    {
        List<WebElement> visibleRepositoryRows = jiraTestedProduct.getTester()
                                                                  .getDriver()
                                                                  .findElements(By.className("dvcs-repo-row"));
        return visibleRepositoryRows.size();
    }
}
