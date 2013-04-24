package it.restart.com.atlassian.jira.plugins.dvcs.test;

import static com.atlassian.jira.plugins.dvcs.pageobjects.BitBucketCommitEntriesAssert.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import it.restart.com.atlassian.jira.plugins.dvcs.DashboardActivityStreamsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraAddUserPage;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.OrganizationDiv;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.pages.DashboardPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.elements.PageElement;

public class BitbucketOrganizationsTest implements BasicOrganizationTests, MissingCommitsTests, ActivityStreamsTest
{
    private static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    private static final String ACCOUNT_NAME = "jirabitbucketconnector";
    private OAuth oAuth;

    
    @BeforeClass
    public void beforeClass()
    {
        // log in to JIRA 
        new JiraLoginPageController(jira).login();
        // log in to Bitbucket
        new MagicVisitor(jira).visit(BitbucketLoginPage.class).doLogin();
        // setup up OAuth from bitbucket
        oAuth = new MagicVisitor(jira).visit(BitbucketOAuthPage.class).addConsumer();
//        jira.visit(JiraBitbucketOAuthPage.class).setCredentials(oAuth.key, oAuth.secret);
    }
    
    @AfterClass
    public void afterClass()
    {
        // delete all organizations
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
        // remove OAuth in bitbucket
        new MagicVisitor(jira).visit(BitbucketOAuthPage.class).removeConsumer(oAuth.applicationId);
        // log out from bitbucket
        new MagicVisitor(jira).visit(BitbucketLoginPage.class).doLogout();
    }
    
    @BeforeMethod
    public void beforeMethod()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
    }


    @Override
    @Test
    public void addOrganization()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(RepositoriesPageController.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        assertThat(organization).isNotNull(); 
        assertThat(organization.getRepositories().size()).isEqualTo(4);  
        
        // check add user extension
        PageElement dvcsExtensionsPanel = jira.visit(JiraAddUserPage.class).getDvcsExtensionsPanel();
        assertThat(dvcsExtensionsPanel.isVisible());
    }
    
    @Override
    @Test
    public void addOrganizationWaitForSync()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(RepositoriesPageController.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);
        
        assertThat(organization).isNotNull(); 
        assertThat(organization.getRepositories().size()).isEqualTo(4);
        assertThat(organization.getRepositories().get(3).getMessage()).isEqualTo("Fri Mar 02 2012");
        
        assertThat(getCommitsForIssue("QA-2", 1)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
        assertThat(getCommitsForIssue("QA-3", 2)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
    }
    
    @Override
    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe url \\[https://privatebitbucket.org\\] is incorrect or the server is not responding.*")
    public void addOrganizationInvalidUrl()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(RepositoriesPageController.BITBUCKET, "https://privatebitbucket.org/someaccount", getOAuthCredentials(), false);
    }
    
    @Override
    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nInvalid user/team account.*")
    public void addOrganizationInvalidAccount()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(RepositoriesPageController.BITBUCKET, "I_AM_SURE_THIS_ACCOUNT_IS_INVALID", getOAuthCredentials(), false);
    }

    @Override
    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe authentication with Bitbucket has failed. Please check your OAuth settings.*")
    public void addOrganizationInvalidOAuth()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(RepositoriesPageController.BITBUCKET, ACCOUNT_NAME, new OAuthCredentials("bad", "credentials"), true);
        
        assertThat(organization).isNotNull(); 
        assertThat(organization.getRepositories().size()).isEqualTo(4);  
    }

    @Test
    @Override
    public void testCommitStatistics()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(RepositoriesPageController.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        // QA-2
        List<BitBucketCommitEntry> commitMessages = getCommitsForIssue("QA-2", 1); // throws AssertionError with other than 1 message

        BitBucketCommitEntry commitMessage = commitMessages.get(0);
        List<PageElement> statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.isAdded(statistics.get(0))).isTrue();

        // QA-3
        commitMessages = getCommitsForIssue("QA-3", 2); // throws AssertionError with other than 2 messages

        // commit 1
        commitMessage = commitMessages.get(0);
        statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.isAdded(statistics.get(0))).isTrue();
        // commit 2
        commitMessage = commitMessages.get(1);
        statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.getAdditions(statistics.get(0))).isEqualTo("+3");
        assertThat(commitMessage.getDeletions(statistics.get(0))).isEqualTo("-");
    }

    @Test
    @Override
    public void testPostCommitHookAdded()
    {
        // remove existing
        String bitbucketServiceConfigUrl = "https://bitbucket.org/!api/1.0/repositories/jirabitbucketconnector/public-hg-repo/services";
        removeExistingServices(bitbucketServiceConfigUrl, "jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));

        // add organization
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(RepositoriesPageController.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        // check postcommit hook is there
        String servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, "jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));
        String baseUrl = jira.getProductInstance().getBaseUrl();
        String syncUrl = baseUrl + "/rest/bitbucket/1.0/repository/";
        assertThat(servicesConfig).contains(syncUrl);

        // delete repository
        rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();

        // check that postcommit hook is removed
        servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, "jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));
        assertThat(servicesConfig).doesNotContain(syncUrl);
    }
    
    @Test
    @Override
    public void testActivityPresentedForQA5()
    {
        // add organization
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(RepositoriesPageController.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        // Activity streams gadget expected at dashboard page!
        DashboardActivityStreamsPage page = jira.visit(DashboardActivityStreamsPage.class);
        assertThat(page.isActivityStreamsGadgetVisible()).isTrue();

        WebElement iframeElm = jira.getTester().getDriver().getDriver().findElement(By.id("gadget-10001"));
        String iframeSrc = iframeElm.getAttribute("src");
        jira.getTester().gotoUrl(iframeSrc);
        
        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);

        // Activity streams should contain at least one changeset with 'more files' link.
        assertThat(page.isMoreFilesLinkVisible()).isTrue();
        page.checkIssueActivityPresentedForQA5();

        page.setIssueKeyFilter("QA-4");
        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);

        // because commit contains both keys QA-4 and QA-5, so should be present on both issues' activity streams
        page.checkIssueActivityPresentedForQA5();

        page.setIssueKeyFilter("QA-5");
        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);

        page.checkIssueActivityPresentedForQA5();

        // delete repository
        rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();

        page = jira.visit(DashboardActivityStreamsPage.class);
        page.checkIssueActivityNotPresentedForQA5();
    }
    
    @Override
    @Test
    public void testAnonymousAccess()
    {
        setupAnonymousAccessAllowed();
        // add organization
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(RepositoriesPageController.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        // Activity streams gadget expected at dashboard page!
        DashboardActivityStreamsPage page = jira.visit(DashboardActivityStreamsPage.class);
        assertThat(page.isActivityStreamsGadgetVisible()).isTrue();

        WebElement iframeElm = jira.getTester().getDriver().getDriver().findElement(By.id("gadget-10001"));
        String iframeSrc = iframeElm.getAttribute("src");
        jira.getTester().gotoUrl(iframeSrc);

        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);
        page.checkIssueActivityPresentedForQA5();

        // logout user
        jira.getTester().getDriver().manage().deleteAllCookies();

        // Activity streams gadget expected at dashboard page!
        jira.visit(DashboardPage.class);
        assertThat(page.isActivityStreamsGadgetVisible()).isTrue();

        jira.getTester().gotoUrl(iframeSrc);
        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);
        page.checkIssueActivityNotPresentedForQA5();

        // log in to JIRA 
        new JiraLoginPageController(jira).login();
        setupAnonymousAccessForbidden();
    }
    
    
    //-------------------------------------------------------------------
    //--------- these methods should go to some common utility/class ---- 
    //-------------------------------------------------------------------
 
    private OAuthCredentials getOAuthCredentials()
    {
        return new OAuthCredentials(oAuth.key, oAuth.secret);
    }

    private List<BitBucketCommitEntry> getCommitsForIssue(String issueKey, int exectedNumberOfCommits)
    {
        return jira.visit(JiraViewIssuePage.class, issueKey)
                .openBitBucketPanel()
                .waitForNumberOfMessages(exectedNumberOfCommits, 1000L, 5);
    }

    private void setupAnonymousAccessAllowed()
    {
        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + "/secure/admin/AddPermission!default.jspa?schemeId=0&permissions=10");
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("type_group"));
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("add_submit"));
        jira.getTester().getDriver().findElement(By.id("type_group")).click();
        jira.getTester().getDriver().findElement(By.id("add_submit")).click();
    }

    private void setupAnonymousAccessForbidden()
    {
        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + "/secure/admin/EditPermissions!default.jspa?schemeId=0");
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("del_perm_10_"));
        jira.getTester().getDriver().findElement(By.id("del_perm_10_")).click();
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("delete_submit"));
        jira.getTester().getDriver().findElement(By.id("delete_submit")).click();
    }
    
    //-------------------------------------------------------------------
    //--------- these methods should go probably to bitbucket client ---- 
    //-------------------------------------------------------------------
    
    private void removeExistingServices(String url, String username, String password)
    {
        String bitbucketServices = getBitbucketServices(url, username, password);
        
        String regexp = "\"id\":.([0-9]*)";
        Matcher m = Pattern.compile(regexp).matcher(bitbucketServices);
        while (m.find())
        {
            String id = m.group(1);
            removeService(url+"/"+id, username, password);
        }
    }
    
    private String removeService(String url, String username, String password)
    {
        HttpMethod method = new DeleteMethod(url);
        return makeHttpRequest(username, password, method);
    }

    private String getBitbucketServices(String url, String username, String password)
    {
        HttpMethod method = new GetMethod(url);
        return makeHttpRequest(username, password, method);
    }

    private String makeHttpRequest(String username, String password, HttpMethod method)
    {
        try
        {
            HttpClient httpClient = new HttpClient();
            
            AuthScope authScope = new AuthScope(method.getURI().getHost(), AuthScope.ANY_PORT, null, AuthScope.ANY_SCHEME);
            httpClient.getParams().setAuthenticationPreemptive(true);
            httpClient.getState().setCredentials(authScope, new UsernamePasswordCredentials(username, password));
            
            httpClient.executeMethod(method);
            return method.getResponseBodyAsString();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

   
}
