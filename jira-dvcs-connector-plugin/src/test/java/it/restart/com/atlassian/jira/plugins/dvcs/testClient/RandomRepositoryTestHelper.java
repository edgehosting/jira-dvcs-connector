package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.base.resource.TimestampNameTestResource;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;

import java.util.ArrayList;
import java.util.Collection;

public abstract class RandomRepositoryTestHelper
{
    protected final Collection<BitbucketRepository> testRepositories = new ArrayList<BitbucketRepository>();
    protected Dvcs dvcs;
    protected OAuth oAuth;
    protected final String userName;
    protected final String password;
    protected final JiraTestedProduct jiraTestedProduct;

    protected RandomRepositoryTestHelper(final String userName, final String password,
            final JiraTestedProduct jiraTestedProduct)
    {
        this.userName = userName;
        this.password = password;
        this.jiraTestedProduct = jiraTestedProduct;
    }

    public abstract void initialiseOrganizationsAndDvcs(final Dvcs dvcs, final OAuth oauth);

    public abstract void cleanupAccountAndOAuth();

    public abstract void setupTestRepository(String repositoryName);

    public abstract void cleanupLocalRepositories(TimestampNameTestResource timestampNameTestResource);

    public abstract AccountsPageAccount.AccountType getAccountType();

    public Dvcs getDvcs()
    {
        return dvcs;
    }

    public OAuth getoAuth()
    {
        return oAuth;
    }
}
