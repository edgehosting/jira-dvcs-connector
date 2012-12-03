package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

/**
 *
 */
public class GithubLoginPage implements Page
{
    public static final String PAGE_URL = "https://github.com/login";

    @ElementBy(id = "login_field")
    PageElement githubWebLoginField;
    
    @ElementBy(id = "logout")
    PageElement githubWebLogoutLink;

    @ElementBy(id = "password")
    PageElement githubWebPasswordField;

    @ElementBy(name = "commit")
    PageElement githubWebSubmitButton;

    @Override
    public String getUrl()
    {
        return PAGE_URL;
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
