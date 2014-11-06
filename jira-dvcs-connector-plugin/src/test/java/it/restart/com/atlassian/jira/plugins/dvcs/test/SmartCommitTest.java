package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.jira.pageobjects.pages.viewissue.link.activity.Comment;
import com.atlassian.jira.plugins.dvcs.base.resource.TimestampNameTestResource;
import com.atlassian.jira.plugins.dvcs.pageobjects.JiraLoginPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.GitDvcs;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.ACCOUNT_NAME;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.PASSWORD;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class SmartCommitTest extends AbstractDVCSTest
{
    protected static final int EXPIRATION_DURATION_5_MIN = 5 * 60 * 1000;
    private static final String COMMIT_AUTHOR = "Jira DvcsConnector";
    private static final String COMMIT_AUTHOR_EMAIL = "jirabitbucketconnector@atlassian.com";

    private static final String TEST_PROJECT_KEY = "TST";

    private TimestampNameTestResource timestampNameTestResource = new TimestampNameTestResource();

    private Dvcs dvcs;

    private String issueKey;

    private String repositoryName;

    private Collection<BitbucketRepository> testRepositories = new ArrayList<BitbucketRepository>();

    @BeforeClass
    public void beforeEachPullRequestTestClass()
    {
        new JiraLoginPageController(getJiraTestedProduct()).login();

        dvcs = new GitDvcs();
        // need to initialize ComponentWorker for UrlBuilder in PullRequestRemoteRestpoint used by BitbucketClient
        //  set the encoding from ApplicationProperties, which is required by UrlBuilder.addPath()
        MockitoAnnotations.initMocks(this);
        final MockComponentWorker mockComponentWorker = new MockComponentWorker().init();
        mockComponentWorker.getMockApplicationProperties().setEncoding("US-ASCII");

        addOrganizations(getJiraTestedProduct());
    }

    @AfterClass (alwaysRun = true)
    public void afterEachPullRequestTestClass()
    {
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(getJiraTestedProduct());
        repositoriesPageController.getPage().deleteAllOrganizations();
        new MagicVisitor(getJiraTestedProduct()).visit(BitbucketOAuthPage.class, ACCOUNT_NAME).removeConsumer(oAuth.applicationId);
    }

    private void addOrganizations(final JiraTestedProduct jiraTestedProduct)
    {
        new MagicVisitor(jiraTestedProduct).visit(BitbucketLoginPage.class).doLogin(ACCOUNT_NAME, PASSWORD);

        // Creates & adds OAuth settings
        oAuth = new MagicVisitor(jiraTestedProduct).visit(BitbucketOAuthPage.class, ACCOUNT_NAME).addConsumer();

        // adds Bitbucket account into Jira
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jiraTestedProduct);
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);
    }

    @BeforeMethod
    protected void beforeEachPullRequestTest()
    {
        repositoryName = timestampNameTestResource.randomName(getRepositoryNameSuffix(), EXPIRATION_DURATION_5_MIN);
        initLocalTestRepository();
        issueKey = addTestIssue(TEST_PROJECT_KEY, getTestIssueSummary());
    }

    protected void initLocalTestRepository()
    {
        BitbucketRemoteClient bbRemoteClient = new BitbucketRemoteClient(ACCOUNT_NAME, PASSWORD);
        RepositoryRemoteRestpoint repositoryService = bbRemoteClient.getRepositoriesRest();

        BitbucketRepository remoteRepository = repositoryService.createRepository(repositoryName, dvcs.getDvcsType(), false);
        testRepositories.add(remoteRepository);
        dvcs.createTestLocalRepository(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);
    }

    @AfterMethod
    protected void afterEachPullRequestTest()
    {
        // delete account in local configuration first to avoid 404 error when uninstalling hook
        dvcs.deleteAllRepositories();

        BitbucketRemoteClient bbRemoteClient = new BitbucketRemoteClient(ACCOUNT_NAME, PASSWORD);
        RepositoryRemoteRestpoint repositoryService = bbRemoteClient.getRepositoriesRest();

        for (BitbucketRepository testRepository : testRepositories)
        {
            repositoryService.removeRepository(testRepository.getOwner(), testRepository.getSlug());
        }
    }

    protected String getRepositoryNameSuffix()
    {
        return this.getClass().getSimpleName().toLowerCase();
    }

    protected String getTestIssueSummary()
    {
        return this.getClass().getCanonicalName();
    }

    @Test
    public void testSimpleCommit() throws InterruptedException
    {
        getJiraTestedProduct().backdoor().usersAndGroups().addUser(COMMIT_AUTHOR, "pass", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL, false);

        // Initial push and sync to get the repository ready
        dvcs.addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        dvcs.commit(ACCOUNT_NAME, repositoryName, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        dvcs.push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, "master", false);

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();
        account.synchronizeRepository(repositoryName);

        String commentText = "this is my comment";
        String smartCommitMessage = issueKey + " #time 2d 2h 2m #comment " + commentText;

        dvcs.addFile(ACCOUNT_NAME, repositoryName, "README2.txt", "Hello World2!".getBytes());
        dvcs.commit(ACCOUNT_NAME, repositoryName, smartCommitMessage, COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        dvcs.push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, "master", false);

        accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        account = accountsPage.getAccount(AccountsPageAccount.AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();
        account.synchronizeRepository(repositoryName);

        ViewIssuePage viewIssuePage = getJiraTestedProduct().goToViewIssue(issueKey);
        Iterable<Comment> comments = viewIssuePage.getComments();
        Iterator<Comment> commentIterator = comments.iterator();

        boolean foundComment = false;
        for (int i = 0; i < 4; i++)
        {
            if (commentIterator.hasNext())
            {
                assertThat(commentIterator.next().getText(), equalTo(commentText));
                foundComment = true;
            }
            viewIssuePage = getJiraTestedProduct().goToViewIssue(issueKey);
            comments = viewIssuePage.getComments();
            commentIterator = comments.iterator();
        }

        assertThat("We should have found the comment during one of the page loads", foundComment);
    }
}
