package com.atlassian.jira.plugins.dvcs.pageobjects.github;


import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.GrantAccessPageController;

public class GithubGrantAccessPageController implements GrantAccessPageController
{
    @Override
    public void grantAccess(JiraTestedProduct jira)
    {
        if (jira.getTester().getDriver().getPageSource().contains("This is not the web page you are looking for"))
        {
            throw new AssertionError("Invalid OAuth");
        }
        
        GithubGrantAccessPage grantAccessPage = jira.getPageBinder().bind(GithubGrantAccessPage.class);
        grantAccessPage.grantAccess();
    }
}
