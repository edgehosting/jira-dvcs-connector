package it.restart.com.atlassian.jira.plugins.dvcs.bitbucket;


import it.restart.com.atlassian.jira.plugins.dvcs.GrantAccessPageController;

import com.atlassian.jira.pageobjects.JiraTestedProduct;

public class BitbucketGrantAccessPageController implements GrantAccessPageController
{
    private final JiraTestedProduct jira;

    public BitbucketGrantAccessPageController(JiraTestedProduct jira)
    {
        this.jira = jira;
    }

    @Override
    public void grantAccess()
    {
        BitbucketGrantAccessPage grantAccessPage = jira.getPageBinder().bind(BitbucketGrantAccessPage.class);
        grantAccessPage.grantAccess();
    }
}