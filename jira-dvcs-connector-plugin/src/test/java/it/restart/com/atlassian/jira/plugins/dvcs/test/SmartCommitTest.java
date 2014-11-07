package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.functest.framework.Administration;
import com.atlassian.jira.functest.framework.Navigation;
import com.atlassian.jira.functest.framework.backdoor.Backdoor;
import com.atlassian.jira.plugins.dvcs.base.resource.TimestampNameTestResource;
import com.atlassian.jira.plugins.dvcs.pageobjects.JiraLoginPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.TimeTrackingPage;
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
    private static final String COMMIT_AUTHOR_EMAIL = "jirabitbucketconnector@atlassian.com";

    private static final String TEST_PROJECT_KEY = "TST";

    private TimestampNameTestResource timestampNameTestResource = new TimestampNameTestResource();

    private String issueKey;

    private String repositoryName;

    private RepositoryTestHelper repositoryTestHelper;

    private Navigation navigation;
    private Administration administration;

    @BeforeClass
    public void beforeClass()
    {
        new JiraLoginPageController(getJiraTestedProduct()).login();

        repositoryTestHelper = new BitbucketRepositoryTestHelper(ACCOUNT_NAME, PASSWORD, getJiraTestedProduct());
        repositoryTestHelper.initialiseOrganizationsAndDvcs(null, null);

        TimeTrackingPage timePage = getJiraTestedProduct().goTo(TimeTrackingPage.class);
        timePage.activateTimeTrackingWithDefaults();

//        final JIRAEnvironmentData environmentData = getJiraTestedProduct().environmentData();
//
//        WebTester webTester = WebTesterFactory.createNewWebTester(environmentData);
////        WebTester webTester = new WebTester();
//        webTester.beginAt("/login.jsp");
//        webTester.setFormElement("os_username", FunctTestConstants.ADMIN_USERNAME);
//        webTester.setFormElement("os_password", FunctTestConstants.ADMIN_PASSWORD);
//        webTester.setWorkingForm("login-form");
//        webTester.submit();
//
//        FuncTestHelperFactory funcTestHelperFactory = new FuncTestHelperFactory(webTester, environmentData);
////        new FuncTestCaseJiraSetup(funcTest, getTester(), environmentData, getNavigation(), webClientListener, skipSetup);
////        new JiraSetupInstanceHelper(webTester, environmentData).ensureJIRAIsReadyToGo(webClientListener);
//
//
//        navigation = funcTestHelperFactory.getNavigation();
//        administration = funcTestHelperFactory.getAdministration();
//
////        navigation = new NavigationImpl(webTester, environmentData);
////        final LocatorFactory locatorFactory = new LocatorFactoryImpl(webTester);
////        final Assertions assertions = new AssertionsImpl(webTester, environmentData, navigation, locatorFactory);
////        administration = new AdministrationImpl(webTester, environmentData, navigation, assertions);
//
//        administration.timeTracking().enable(TimeTracking.Mode.MODERN);
//
//        System.out.println("enabled");
////        try
////        {
////            Thread.sleep(40000);
////        }
////        catch (InterruptedException e)
////        {
////            throw new RuntimeException(e);
////        }

        final Backdoor backdoor = getJiraTestedProduct().backdoor();
        backdoor.usersAndGroups().addUser(COMMIT_AUTHOR, "pass", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL, false);
        backdoor.usersAndGroups().addUserToGroup(COMMIT_AUTHOR, "jira-developers");
    }

    @AfterClass (alwaysRun = true)
    public void afterClass()
    {
        TimeTrackingPage timePage = getJiraTestedProduct().goTo(TimeTrackingPage.class);
        timePage.deactivateTimeTrackingWithDefaults();

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
    public void runSmartCommitTest() throws InterruptedException
    {
//        getJiraTestedProduct().backdoor().advancedSettings()

        String ignoredSmartCommitMessage = issueKey + " #comment This smart commit should be ignored";
        Dvcs dvcs = repositoryTestHelper.getDvcs();
        addCommit(dvcs, ignoredSmartCommitMessage);

        System.out.println("first sync");

        // The first sync of the account will not trigger smart commits.
        AccountsPage.refreshAccountAndSync(getJiraTestedProduct(), AccountsPageAccount.AccountType.BITBUCKET,
                ACCOUNT_NAME, repositoryName);

//        Thread.sleep(20000);

        String commentText = "this is my comment";
        String timeSpent = "2d 2h 2m";
        String smartCommitMessage = issueKey + " #Resolve #time " + timeSpent + " #comment " + commentText;
        addCommit(dvcs, smartCommitMessage);
        AccountsPage.syncAccount(getJiraTestedProduct(), AccountsPageAccount.AccountType.BITBUCKET,
                ACCOUNT_NAME, repositoryName);

        System.out.println("second sync");

//        Thread.sleep(700000);

        boolean foundComment = false;
        // Can take a while for smart commits to be processed, we will retry a few times
        for (int i = 0; i < 20; i++)
        {
            System.out.println("checking for smart commit run " + i);
            Issue issue = getJiraTestedProduct().backdoor().issues().getIssue(issueKey);


            System.out.println("fetched issue");


//            for(IssueTransitionsMeta.Transition transition : issue.transitions){
//                System.out.println("transition " + transition.name);
//            }
//
//            System.out.println("printed trans");

            Status status = issue.fields.get("status");

            System.out.println("status is " + status + " id " + status.id());

            if ("Resolved".equalsIgnoreCase(status.name()))
            {
                System.out.println("got the issue, waiting");
//                try
//                {
//                    Thread.sleep(500);
//                }
//                catch (InterruptedException e)
//                {
//                }
//                issue = getJiraTestedProduct().backdoor().issues().getIssue(issueKey);
                foundComment = true;
                assertThat("should be this many comments logged", issue.getComments().size(), equalTo(1));
                Comment comment = issue.getComments().get(0);
                assertThat(comment.body, equalTo(commentText));
                WorklogWithPaginationBean worklog = issue.fields.get("worklog");
                assertThat("should be this many work items logged", worklog.worklogs.size(), equalTo(1));
                Worklog workItem = worklog.worklogs.get(0);
                assertThat("time spent", workItem.timeSpent, equalTo(timeSpent));
                return;
            }

            try
            {
                Thread.sleep(50);
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
