package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

/**
 *
 */
public class GithubLoginPage implements Page
{
    public static final String PAGE_URL = "https://github.com/login";
    public static final String LOGOUT_ACTION_URL = "https://github.com/logout";

    @ElementBy(id = "login_field")
    PageElement githubWebLoginField;

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
        githubWebLoginField.type("jirabitbucketconnector");
        githubWebPasswordField.type("dvcsconnector23");
        githubWebSubmitButton.click();
    }
}
