package it.restart.com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class GithubLoginPage implements Page
{
    @ElementBy(id = "login_field")
    private PageElement githubWebLoginField;
    
    @ElementBy(id = "logout")
    private PageElement githubWebLogoutLink;

    @ElementBy(id = "password")
    private PageElement githubWebPasswordField;

    @ElementBy(name = "commit")
    private PageElement githubWebSubmitButton;

    @Override
    public String getUrl()
    {
        return "https://github.com/login";
    }

    public void doLogin()
    {
        doLogin("jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));
    }
    
    public void doLogin(String username, String password)
    {
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
        githubWebLogoutLink.click();
    }
}
