package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.functest.framework.backdoor.Backdoor;
import com.atlassian.jira.plugins.dvcs.base.resource.TimestampNameTestResource;
import com.atlassian.jira.plugins.dvcs.pageobjects.JiraLoginPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.TimeTrackingAdminPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.testkit.client.restclient.Comment;
import com.atlassian.jira.testkit.client.restclient.Issue;
import com.atlassian.jira.testkit.client.restclient.Status;
import com.atlassian.jira.testkit.client.restclient.Worklog;
import com.atlassian.jira.testkit.client.restclient.WorklogWithPaginationBean;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.BitbucketRepositoryTestHelper;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.RepositoryTestHelper;
import it.util.TestAccounts;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.ACCOUNT_NAME;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.PASSWORD;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.Assert.fail;

public class SmartCommitTest extends AbstractDVCSTest
{
    private static final int EXPIRATION_DURATION_5_MIN = 5 * 60 * 1000;
    private static final String COMMIT_AUTHOR = "Jira DvcsConnector";
    private static final String COMMIT_AUTHOR_EMAIL = TestAccounts.JIRA_BB_CONNECTOR_ACCOUNT +"@atlassian.com";
    private static final String TEST_PROJECT_KEY = "TST";


    private TimestampNameTestResource timestampNameTestResource = new TimestampNameTestResource();

    private String issueKey;

    private String repositoryName;

    private RepositoryTestHelper repositoryTestHelper;

    @BeforeClass
    public void beforeClass()
    {
        getJiraTestedProduct().backdoor().restoreDataFromResource(TEST_DATA);
        new JiraLoginPageController(getJiraTestedProduct()).login();

        repositoryTestHelper = new BitbucketRepositoryTestHelper(ACCOUNT_NAME, PASSWORD, getJiraTestedProduct());
        repositoryTestHelper.initialiseOrganizationsAndDvcs(null, null);

        TimeTrackingAdminPage timePage = getJiraTestedProduct().goTo(TimeTrackingAdminPage.class);
        timePage.activateTimeTrackingWithDefaults();

        final Backdoor backdoor = getJiraTestedProduct().backdoor();
        backdoor.usersAndGroups().addUserEvenIfUserExists(COMMIT_AUTHOR, "pass", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL, false);
        backdoor.usersAndGroups().addUserToGroup(COMMIT_AUTHOR, "jira-developers");
    }

    @AfterClass (alwaysRun = true)
    public void afterClass()
    {
        TimeTrackingAdminPage timePage = getJiraTestedProduct().goTo(TimeTrackingAdminPage.class);
        timePage.deactivateTimeTrackingWithDefaults();

        repositoryTestHelper.deleteAllOrganizations();
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
    public void runSmartCommitTest() throws InterruptedException
    {
        String ignoredSmartCommitMessage = issueKey + " #comment This smart commit should be ignored";
        Dvcs dvcs = repositoryTestHelper.getDvcs();
        addCommit(dvcs, ignoredSmartCommitMessage);

        // The first sync of the account will not trigger smart commits.
        AccountsPage.syncAccount(getJiraTestedProduct(), AccountsPageAccount.AccountType.BITBUCKET,
                ACCOUNT_NAME, repositoryName, true);

        String commentText = "this is my comment";
        String timeSpent = "2d 2h 2m";
        String smartCommitMessage = issueKey + " #Resolve #time " + timeSpent + " #comment " + commentText;
        addCommit(dvcs, smartCommitMessage);
        AccountsPage.syncAccount(getJiraTestedProduct(), AccountsPageAccount.AccountType.BITBUCKET,
                ACCOUNT_NAME, repositoryName, false);

        boolean issueIsResolved = false;
        boolean issueHasComment = false;
        boolean workHasBeenLogged = false;
        // Can take a while for smart commits to be processed, we will retry a few times
        for (int i = 0; i < 20; i++)
        {
            System.out.println("checking for smart commit run " + i);
            Issue issue = getJiraTestedProduct().backdoor().issues().getIssue(issueKey);

            Status status = issue.fields.get("status");

            if ("Resolved".equalsIgnoreCase(status.name()))
            {
                issueIsResolved = true;
            }

            if (issue.getComments().size() == 1)
            {
                Comment comment = issue.getComments().get(0);
                assertThat(comment.body, equalTo(commentText));
                issueHasComment = true;
            }

            WorklogWithPaginationBean worklog = issue.fields.get("worklog");
            if (worklog != null && worklog.worklogs.size() == 1)
            {
                Worklog workItem = worklog.worklogs.get(0);
                assertThat("time spent", workItem.timeSpent, equalTo(timeSpent));
                workHasBeenLogged = true;
            }

            if (issueIsResolved && issueHasComment && workHasBeenLogged)
            {
                // all done, finish the test
                return;
            }

            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
            }
        }

        fail("should have found the smart commit");
    }

    private void addCommit(Dvcs dvcs, String commitMessage)
    {
        dvcs.addFile(ACCOUNT_NAME, repositoryName, "README2.txt", "Hello World2!".getBytes());
        dvcs.commit(ACCOUNT_NAME, repositoryName, commitMessage, COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        dvcs.push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, "master", false);
    }
}
