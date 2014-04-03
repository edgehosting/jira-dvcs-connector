package it.restart.com.atlassian.jira.plugins.dvcs.bitbucket;

import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.WebDriverException;

public class BitbucketLoginPage implements Page
{
    @ElementBy(id = "id_username")
    private PageElement usernameOrEmailInput;

    @ElementBy(id = "id_password")
    private PageElement passwordInput;
    
    @ElementBy(name = "submit")
    private PageElement loginButton;

    @ElementBy(id = "user-dropdown-trigger")
    private PageElement userDropdownTriggerLink;
    
    @ElementBy(linkText = "Log out")
    private PageElement logoutLink;

    @Override
    public String getUrl()
    {
        return "https://bitbucket.org/account/signin/?next=/";
    }

    public void doLogin()
    {
        doLogin("jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));
    }

    public void doLogin(String username, String password)
    {
        try
        {
            usernameOrEmailInput.clear().type(username);
        }
        catch(WebDriverException e)
        {
            // workaround for Permission denied to access property 'nr@context' issue
            usernameOrEmailInput.clear().type(username);
        }
        passwordInput.clear().type(password);
        loginButton.click();
    }
    
    public void doLogout()
    {
        try
        {
            userDropdownTriggerLink.click();
        }
        catch(WebDriverException e)
        {
            // workaround for Permission denied to access property 'nr@context' issue
            userDropdownTriggerLink.click();
        }

        logoutLink.click();
    }
    
}
