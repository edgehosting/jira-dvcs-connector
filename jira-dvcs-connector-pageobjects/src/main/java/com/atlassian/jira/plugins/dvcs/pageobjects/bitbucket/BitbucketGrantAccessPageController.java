package com.atlassian.jira.plugins.dvcs.pageobjects.bitbucket;


import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.GrantAccessPageController;

public class BitbucketGrantAccessPageController implements GrantAccessPageController
{
    @Override
    public void grantAccess(JiraTestedProduct jira)
    {
        BitbucketGrantAccessPage grantAccessPage = jira.getPageBinder().bind(BitbucketGrantAccessPage.class);
        grantAccessPage.grantAccess();
    }
}
