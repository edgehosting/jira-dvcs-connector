package it.restart.com.atlassian.jira.plugins.dvcs.bitbucket;

import com.atlassian.jira.plugins.dvcs.pageobjects.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

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

    public void doLogin(String username, String password)
    {
        // accessing tag name as workaround for permission denied to access property 'nr@context' issue
        PageElementUtils.permissionDeniedWorkAround(usernameOrEmailInput);

        usernameOrEmailInput.clear().type(username);
        passwordInput.clear().type(password);
        loginButton.click();
    }
    
    public void doLogout()
    {
        // accessing tag name as workaround for permission denied to access property 'nr@context' issue
        PageElementUtils.permissionDeniedWorkAround(usernameOrEmailInput);

        userDropdownTriggerLink.click();
        logoutLink.click();
    }
    
}
