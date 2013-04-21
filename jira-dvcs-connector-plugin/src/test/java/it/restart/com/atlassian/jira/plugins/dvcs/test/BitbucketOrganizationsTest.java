package it.restart.com.atlassian.jira.plugins.dvcs.test;

import static com.atlassian.jira.plugins.dvcs.pageobjects.BitBucketCommitEntriesAssert.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.elements.PageElement;

public class BitbucketOrganizationsTest implements BasicOrganizationTests, MissingCommitsTests
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
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(RepositoriesPageController.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);
        
        String baseUrl = jira.getProductInstance().getBaseUrl();
        String syncUrl = baseUrl + "/rest/bitbucket/1.0/repository/";
        String bitbucketServiceConfigUrl = "https://bitbucket.org/!api/1.0/repositories/jirabitbucketconnector/public-hg-repo/services";
        String servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, "jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));
        assertThat(servicesConfig).contains(syncUrl);
        // delete repository
        rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
        // check that postcommit hook is removed
        servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, "jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));
        assertThat(servicesConfig).doesNotContain(syncUrl);
    }

    //-------------------------------------------------------------------
    //--------- these methods should go to some common utility/class ---- 
    //-------------------------------------------------------------------
    
    private String getBitbucketServices(String url, String username, String password)
    {
        try
        {
            HttpClient httpClient = new HttpClient();
            HttpMethod method = new GetMethod(url);
            
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

}
