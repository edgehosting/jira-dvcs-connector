package it.restart.com.atlassian.jira.plugins.dvcs;

import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketGrantAccessPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.PageController;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraPageUtils;

public class RepositoriesPageController implements PageController<RepositoriesPage>
{
    public static final int ACCOUNT_TYPE_BITBUCKET = 0;
    public static final int ACCOUNT_TYPE_GITHUB = 1;
    public static final int ACCOUNT_TYPE_GITHUBENTERPRISE = 2;
    private final JiraTestedProduct jira;
    private final RepositoriesPage page;

    public RepositoriesPageController(JiraTestedProduct jira)
    {
        this.jira = jira;
        this.page = jira.getPageBinder().navigateToAndBind(RepositoriesPage.class);
    }
    
    @Override
    public RepositoriesPage getPage()
    {
        return page;
    }

    public void addOrganization(int accountType, String accountName, boolean autosync)
    {
        page.addOrganisation(accountType, accountName, autosync);
        
        if(requiresGrantAccess())
        {
            getGrantAccessController(accountType).grantAccess();
        }
        
        if (autosync)
        {
            waitForSyncToFinish();
        }
        
//        Poller.waitUntilFalse(atlassianTokenMeta.timed().isPresent());
//        pageBinder.bind(BitbucketGrandOAuthAccessPage.class).grantAccess();
//        Poller.waitUntilTrue(atlassianTokenMeta.timed().isPresent());

    }

    private void waitForSyncToFinish()
    {
        JiraPageUtils.checkSyncProcessSuccess(jira);
    }

    private boolean requiresGrantAccess()
    {
        // if access has been granted before browser will 
        // redirect immediately back to jira
        String currentUrl = jira.getTester().getDriver().getCurrentUrl();
        return !currentUrl.contains("/jira");
    }

    private GrantAccessPageController getGrantAccessController(int accountType)
    {
        switch (accountType)
        {
        case ACCOUNT_TYPE_BITBUCKET:
            return new BitbucketGrantAccessPageController(jira);
        case ACCOUNT_TYPE_GITHUB:
            return new BitbucketGrantAccessPageController(jira);
        case ACCOUNT_TYPE_GITHUBENTERPRISE:
            return new BitbucketGrantAccessPageController(jira);
        }
        return null;
    }

}
