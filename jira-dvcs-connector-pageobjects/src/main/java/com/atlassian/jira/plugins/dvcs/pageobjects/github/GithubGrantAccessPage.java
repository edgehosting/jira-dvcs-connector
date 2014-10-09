package com.atlassian.jira.plugins.dvcs.pageobjects.github;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class GithubGrantAccessPage implements Page
{    

    @ElementBy(name = "authorize")
    PageElement githubWebAuthorizeButton;

    @Override
    public String getUrl()
    {
        throw new UnsupportedOperationException();
    }
    
    public void grantAccess()
    {
        githubWebAuthorizeButton.click();
    }
}
