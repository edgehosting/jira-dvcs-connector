package it.restart.com.atlassian.jira.plugins.dvcs;

import static org.fest.assertions.api.Assertions.assertThat;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketGrantAccessPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.PageController;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubGrantAccessPageController;

import java.util.List;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;

public class RepositoriesPageController implements PageController<RepositoriesPage>
{
    
    private final JiraTestedProduct jira;
    private final RepositoriesPage page;

    public RepositoriesPageController(JiraTestedProduct jira)
    {
        this.jira = jira;
        this.page = jira.visit(RepositoriesPage.class);
    }
    
    @Override
    public RepositoriesPage getPage()
    {
        return page;
    }

    public OrganizationDiv addOrganization(AccountType accountType, String accountName, OAuthCredentials oAuthCredentials, boolean autosync)
    {
        return addOrganization(accountType, accountName, null, oAuthCredentials, autosync);
    }
    
    public OrganizationDiv addOrganization(AccountType accountType, String accountName, String url, OAuthCredentials oAuthCredentials, boolean autosync)
    {
        page.addOrganisation(accountType.index, accountName, url, oAuthCredentials, autosync);
        assertThat(page.getErrorStatusMessage()).isNull();
        if(requiresGrantAccess())
        {
            accountType.grantAccessPageController.grantAccess(jira);
        }
        
        OrganizationDiv organization = page.getOrganization(accountType.type, accountName);
        if (autosync)
        {
            waitForSyncToFinish(organization);
        } else
        {
            assertThat(isSyncFinished(organization));
        }
        return organization;
    }

    public void waitForSyncToFinish(OrganizationDiv organization)
    {
        do
        {
            try
            {
                Thread.sleep(1000l);
            } catch (InterruptedException e)
            {
                // ignore
            }
        } while (!isSyncFinished(organization));
    }
    
    private boolean isSyncFinished(OrganizationDiv organization)
    {
        List<RepositoryDiv> repositories = organization.getRepositories();
        for (RepositoryDiv repositoryDiv : repositories)
        {
            if (repositoryDiv.isSyncing())
            {
                return false;
            }
        }
        return true;
    }

    private boolean requiresGrantAccess()
    {
        // if access has been granted before browser will 
        // redirect immediately back to jira
        String currentUrl = jira.getTester().getDriver().getCurrentUrl();
        return !currentUrl.contains("/jira");
    }


    /**
    *
    */
    public static final AccountType BITBUCKET = new AccountType(0, "bitbucket", null, new BitbucketGrantAccessPageController());
    public static final AccountType GITHUB = new AccountType(1, "github", null, new GithubGrantAccessPageController());
    public static AccountType getGHEAccountType(String hostUrl)
    {
        return new AccountType(2, "github1", hostUrl, null); // TODO GrantAccessPageController
    }

    static class AccountType
    {
        public final int index;
        public final String type;
        public final GrantAccessPageController grantAccessPageController;
        public final String hostUrl;

        private AccountType(int index, String type, String hostUrl, GrantAccessPageController grantAccessPageController)
        {
            this.index = index;
            this.type = type;
            this.hostUrl = hostUrl;
            this.grantAccessPageController = grantAccessPageController;
        }

    }
    
}
