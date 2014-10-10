package it.restart.com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

import javax.inject.Inject;

public class GithubLoginPage implements Page
{
    @ElementBy(id = "login_field")
    private PageElement githubWebLoginField;

    @ElementBy(id = "logout")
    private PageElement oldGithubWebLogoutLink;

    @ElementBy(xpath = "//*[@aria-label='Sign out']")
    private PageElement githubWebLogoutLink;

    @ElementBy(id = "password")
    private PageElement githubWebPasswordField;

    @ElementBy(name = "commit")
    private PageElement githubWebSubmitButton;

    @ElementBy(xpath = "//input[@value='Sign out']")
    private PageElement getGithubWebLogoutConfirm;

    @Inject
    private JiraTestedProduct jiraTestedProduct;

    private final String hostUrl;

    
    public GithubLoginPage()
    {
        this("https://github.com");
    }
    
    public GithubLoginPage(String hostUrl)
    {
        this.hostUrl = hostUrl;
    }

    @Override
    public String getUrl()
    {
        return hostUrl+"/login";
    }

    public void doLogin()
    {
        doLogin("jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));
    }
    
    public void doLogin(String username, String password)
    {
        // if logout link is present, other user remained logged in
        if (githubWebLogoutLink.isPresent())
        {
            doLogout();
            jiraTestedProduct.getTester().gotoUrl(getUrl());
        }

        githubWebLoginField.type(username);
        githubWebPasswordField.type(password);
        githubWebSubmitButton.click();
    }
    
    /**
     * Logout is done by POST method. It's not enough to go to /logout page. We
     * need to submit a form that gets us there
     */
    public void doLogout()
    {
        if (githubWebLogoutLink.isPresent())
        {
            githubWebLogoutLink.click();
        }
        else if (oldGithubWebLogoutLink.isPresent())
        {
            oldGithubWebLogoutLink.click();
        }
        else
        {
            return; // skip if user has already logged out
        }
        try
        {
            // GitHub sometimes requires logout confirm
            Poller.waitUntilTrue(getGithubWebLogoutConfirm.timed().isPresent());
            getGithubWebLogoutConfirm.click();
        }
        catch (AssertionError e)
        {
            // GitHub doesn't requires logout confirm
        }
    }
}
