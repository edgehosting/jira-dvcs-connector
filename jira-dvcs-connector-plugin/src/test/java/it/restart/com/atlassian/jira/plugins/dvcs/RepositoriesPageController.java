package it.restart.com.atlassian.jira.plugins.dvcs;

import static org.fest.assertions.api.Assertions.assertThat;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketGrantAccessPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.PageController;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubGrantAccessPageController;

import java.util.List;

import com.atlassian.jira.pageobjects.JiraTestedProduct;

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

    public OrganizationDiv addOrganization(AccountType accountType, String accountName, boolean autosync)
    {
        page.addOrganisation(accountType.index, accountName, autosync);
        assertThat(page.getErrorStatusMessage()).isNull();
        if (requiresGrantAccess())
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

    /**
     * Enables provided repository.
     * 
     * @param accountType
     *            type of repository
     * @param accountName
     *            under which account
     * @param repositoryName
     *            name of repository
     */
    public void enable(AccountType accountType, String accountName, String repositoryName)
    {
        OrganizationDiv organization = page.getOrganization(accountType.type, accountName);
        OrganizationRepositoryRow repository = organization.getRepository(repositoryName);
        if (!repository.isEnabled()) {
            repository.enable();
        }
    }

    /**
     * Synchronizes provided repository.
     * 
     * @param accountType
     *            type of repository
     * @param accountName
     *            under which account
     * @param repositoryName
     *            name of repository
     */
    public void synchronize(AccountType accountType, String accountName, String repositoryName)
    {
        OrganizationDiv organization = page.getOrganization(accountType.type, accountName);
        OrganizationRepositoryRow repository = organization.getRepository(repositoryName);
        repository.synchronize();
        waitForSyncToFinish(repository);
    }

    /**
     * Waiting until synchronization is done.
     * 
     * @param repository
     *            on which repository we are waiting
     */
    private void waitForSyncToFinish(OrganizationRepositoryRow repository)
    {
        do
        {
            try
            {
                Thread.sleep(1000l);
            } catch (InterruptedException e)
            {
                // nothing to do
            }
        } while (repository.isSyncing());
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
        List<OrganizationRepositoryRow> repositories = organization.getRepositories();
        for (OrganizationRepositoryRow repositoryDiv : repositories)
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
    public static final AccountType BITBUCKET = new AccountType(0, "bitbucket", new BitbucketGrantAccessPageController());
    public static final AccountType GITHUB = new AccountType(1, "github", new GithubGrantAccessPageController());
    public static final AccountType GITHUBENTERPRISE = new AccountType(2, "githube", new GithubGrantAccessPageController());

    static class AccountType
    {
        public final int index;
        public final String type;
        public final GrantAccessPageController grantAccessPageController;

        private AccountType(int index, String type, GrantAccessPageController grantAccessPageController)
        {
            this.index = index;
            this.type = type;
            this.grantAccessPageController = grantAccessPageController;
        }

    }

}
