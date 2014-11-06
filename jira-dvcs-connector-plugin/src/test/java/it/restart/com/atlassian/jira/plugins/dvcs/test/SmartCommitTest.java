package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.jira.pageobjects.pages.viewissue.link.activity.Comment;
import com.atlassian.jira.plugins.dvcs.base.resource.TimestampNameTestResource;
import com.atlassian.jira.plugins.dvcs.pageobjects.JiraLoginPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.BitbucketRandomRepositoryTestHelper;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.RandomRepositoryTestHelper;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Iterator;

import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.ACCOUNT_NAME;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.PASSWORD;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class SmartCommitTest extends AbstractDVCSTest
{
    private static final int EXPIRATION_DURATION_5_MIN = 5 * 60 * 1000;
    private static final String COMMIT_AUTHOR = "Jira DvcsConnector";
    private static final String COMMIT_AUTHOR_EMAIL = "jirabitbucketconnector@atlassian.com";

    private static final String TEST_PROJECT_KEY = "TST";

    private TimestampNameTestResource timestampNameTestResource = new TimestampNameTestResource();

    private String issueKey;

    private String repositoryName;

    private RandomRepositoryTestHelper repositoryTestHelper;

    @BeforeClass
    public void beforeClass()
    {
        new JiraLoginPageController(getJiraTestedProduct()).login();
        getJiraTestedProduct().backdoor().usersAndGroups().addUser(COMMIT_AUTHOR, "pass", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL, false);

        repositoryTestHelper = new BitbucketRandomRepositoryTestHelper(ACCOUNT_NAME, PASSWORD, getJiraTestedProduct());
        repositoryTestHelper.initialiseOrganizationsAndDvcs(null, null);
    }

    @AfterClass (alwaysRun = true)
    public void afterClass()
    {
        repositoryTestHelper.cleanupAccountAndOAuth();
    }

    @BeforeMethod
    protected void beforeTest()
    {
        repositoryName = timestampNameTestResource.randomName(this.getClass().getSimpleName().toLowerCase(),
                EXPIRATION_DURATION_5_MIN);
        repositoryTestHelper.setupTestRepository(repositoryName);
        issueKey = addTestIssue(TEST_PROJECT_KEY, this.getClass().getCanonicalName());
    }

    @AfterMethod
    protected void afterTest()
    {
        repositoryTestHelper.cleanupLocalRepositories(timestampNameTestResource);
    }

    @Test
    public void smartCommitsShouldBeProcessed()
    {
        runSmartCommitTest(true);
    }

    @Test
    public void smartCommitsShouldNotBeProcessed()
    {
        runSmartCommitTest(false);
    }

    public void runSmartCommitTest(boolean shouldProcessSmartCommit)
    {
        Dvcs dvcs = repositoryTestHelper.getDvcs();

        // Initial push and sync to get the repository ready
        dvcs.addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        dvcs.commit(ACCOUNT_NAME, repositoryName, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        dvcs.push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, "master", false);

        if (shouldProcessSmartCommit)
        {
            AccountsPage.refreshAccountAndSync(getJiraTestedProduct(), AccountsPageAccount.AccountType.BITBUCKET,
                    ACCOUNT_NAME, repositoryName);
        }
        String commentText = "this is my comment";
        String smartCommitMessage = issueKey + " #time 2d 2h 2m #comment " + commentText;

        dvcs.addFile(ACCOUNT_NAME, repositoryName, "README2.txt", "Hello World2!".getBytes());
        dvcs.commit(ACCOUNT_NAME, repositoryName, smartCommitMessage, COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        dvcs.push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, "master", false);

        AccountsPage.refreshAccountAndSync(getJiraTestedProduct(), AccountsPageAccount.AccountType.BITBUCKET,
                ACCOUNT_NAME, repositoryName);

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
                break;
            }
            viewIssuePage = getJiraTestedProduct().goToViewIssue(issueKey);
            comments = viewIssuePage.getComments();
            commentIterator = comments.iterator();
        }

        if (shouldProcessSmartCommit)
        {
            assertThat("Smart commits were meant to be enabled", foundComment);
        }
        else
        {
            assertThat("Smart commits were meant to be disabled", !foundComment);
        }
    }
}
