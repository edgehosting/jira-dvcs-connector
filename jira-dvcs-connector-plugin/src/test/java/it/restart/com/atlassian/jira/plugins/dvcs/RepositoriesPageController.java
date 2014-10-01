package it.restart.com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.RepositoryList;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.RepositoriesLocalRestpoint;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketGrantAccessPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.PageController;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubGrantAccessPageController;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

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
        OrganizationDiv existingOrganisation = page.getOrganization(accountType.type, accountName);
        if (existingOrganisation != null)
        {
            // Org shouldn't be there lets clean it up
            existingOrganisation.delete();
        }
        return addOrganization(accountType, accountName, oAuthCredentials, autosync, false);
    }

    public OrganizationDiv addOrganization(AccountType accountType, String accountName, OAuthCredentials oAuthCredentials, boolean autosync, boolean expectError)
    {
        page.addOrganisation(accountType.index, accountName, accountType.hostUrl, oAuthCredentials, autosync);
        assertThat(page.getErrorStatusMessage()).isNull();

        if ("githube".equals(accountType.type))
        {
            // Confirm submit for GitHub Enterprise
            // "Please be sure that you are logged in to GitHub Enterprise before clicking "Continue" button."
            page.continueAddOrgButton.click();
        }

        if (requiresGrantAccess())
        {
            accountType.grantAccessPageController.grantAccess(jira);
        }

        assertThat(page.getErrorStatusMessage()).isNull();

        if (expectError)
        {
            // no need to repeat the rest of the steps if we expect error but do not see it
            return null;
        }

        OrganizationDiv organization = page.getOrganization(accountType.type, accountName);
        if (autosync)
        {
            waitForSyncToFinish();
            if (!getSyncErrors().isEmpty())
            {
                // refreshing account to retry synchronization
                organization.refresh();
                waitForSyncToFinish();
            }
            assertThat(getSyncErrors()).describedAs("Synchronization failed").isEmpty();
        }
        else
        {
            assertThat(isSyncFinished());
        }
        return organization;
    }

    /**
     * Waiting until synchronization is done.
     */
    public void waitForSyncToFinish()
    {
        do
        {
            try
            {
                Thread.sleep(1000l);
            }
            catch (InterruptedException e)
            {
                // ignore
            }
        }
        while (!isSyncFinished());
    }

    private boolean isSyncFinished()
    {
        RepositoryList repositories = new RepositoriesLocalRestpoint().getRepositories();
        for (Repository repository : repositories.getRepositories())
        {
            if (repository.getSync() != null && !repository.getSync().isFinished())
            {
                return false;
            }
        }
        return true;
    }

    private List<String> getSyncErrors()
    {
        List<String> errors = new ArrayList<String>();
        RepositoryList repositories = new RepositoriesLocalRestpoint().getRepositories();
        for (Repository repository : repositories.getRepositories())
        {
            if (repository.getSync() != null && repository.getSync().getError() != null)
            {
                errors.add(repository.getSync().getError());
            }
        }
        return errors;
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
    public static class AccountType
    {
        public static final AccountType BITBUCKET = new AccountType(0, "bitbucket", null, new BitbucketGrantAccessPageController());
        public static final AccountType GITHUB = new AccountType(1, "github", null, new GithubGrantAccessPageController());

        public static AccountType getGHEAccountType(String hostUrl)
        {
            return new AccountType(2, "githube", hostUrl, new GithubGrantAccessPageController()); // TODO GrantAccessPageController
        }

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
