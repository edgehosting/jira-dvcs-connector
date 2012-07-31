package com.atlassian.jira.plugins.dvcs.pageobjects.page;

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
        usernameOrEmailInput.clear()
                            .type("jirabitbucketconnector");
        passwordInput.clear()
                     .type("jirabitbucketconnector1");
        
        loginButton.click();
    }
}
