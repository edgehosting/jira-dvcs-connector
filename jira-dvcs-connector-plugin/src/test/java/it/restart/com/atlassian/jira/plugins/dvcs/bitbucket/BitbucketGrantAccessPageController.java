package it.restart.com.atlassian.jira.plugins.dvcs.bitbucket;


import it.restart.com.atlassian.jira.plugins.dvcs.GrantAccessPageController;

import com.atlassian.jira.pageobjects.JiraTestedProduct;

public class BitbucketGrantAccessPageController implements GrantAccessPageController
{
    @Override
    public void grantAccess(JiraTestedProduct jira)
    {
        BitbucketGrantAccessPage grantAccessPage = jira.getPageBinder().bind(BitbucketGrantAccessPage.class);
        grantAccessPage.grantAccess();
    }
}