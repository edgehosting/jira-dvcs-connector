package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public class BitbucketLoginPage implements Page {

    public static String LOGIN_PAGE = "https://bitbucket.org/account/signin/?next=/";
    
    
    @ElementBy(id = "id_username")
    PageElement usernameOrEmailInput;
    
    @ElementBy(id = "id_password")
    PageElement passwordInput;
    
    @ElementBy(name= "submit")
    PageElement loginButton;
    
    
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
        usernameOrEmailInput.clear()
                            .type(username);
        passwordInput.clear()
                     .type(password);
        
        loginButton.click();
    }
}
