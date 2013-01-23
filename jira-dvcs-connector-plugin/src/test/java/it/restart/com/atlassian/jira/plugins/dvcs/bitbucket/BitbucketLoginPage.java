package it.restart.com.atlassian.jira.plugins.dvcs.bitbucket;

import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class BitbucketLoginPage implements Page
{
    public static String LOGIN_PAGE = "https://bitbucket.org/account/signin/?next=/";

    @ElementBy(id = "id_username")
    PageElement usernameOrEmailInput;

    @ElementBy(id = "id_password")
    PageElement passwordInput;
    
    @ElementBy(name = "submit")
    PageElement loginButton;

    @ElementBy(id = "user-dropdown-trigger")
    PageElement userDropdownTriggerLink;
    
    @ElementBy(linkText = "Log out")
    PageElement logOutLink;

    @Override
    public String getUrl()
    {
        return LOGIN_PAGE;
    }

    public void doLogin()
    {
        doLogin("jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));
    }

    public void doLogin(String username, String password)
    {
        usernameOrEmailInput.clear().type(username);
        passwordInput.clear().type(password);

        loginButton.click();
    }
    
    public void doLogout()
    {
        userDropdownTriggerLink.click();
        logOutLink.click();
    }
    
}
