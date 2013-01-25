package it.restart.com.atlassian.jira.plugins.dvcs.github;


import it.restart.com.atlassian.jira.plugins.dvcs.GrantAccessPageController;

import com.atlassian.jira.pageobjects.JiraTestedProduct;

public class GithubGrantAccessPageController implements GrantAccessPageController
{
    @Override
    public void grantAccess(JiraTestedProduct jira)
    {
        GithubGrantAccessPage grantAccessPage = jira.getPageBinder().bind(GithubGrantAccessPage.class);
        grantAccessPage.grantAccess();
    }
}