package it.com.atlassian.jira.plugins.dvcs;

import static com.atlassian.jira.plugins.dvcs.pageobjects.BitBucketCommitEntriesAssert.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.openqa.selenium.By;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BaseConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitBucketConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketIntegratedApplicationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthConfigPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraAddUserPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceField;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ServiceRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.elements.PageElement;

/**
 * Test to verify behaviour when syncing bitbucket repository..
 */
public class BitbucketOrganzationsTest extends BaseOrganizationTest<BitBucketConfigureOrganizationsPage>
{
    private static final String TEST_ORGANIZATION = "jirabitbucketconnector";
    private static final String TEST_NOT_EXISTING_URL = "https://privatebitbucket.org/someaccount";
    private static final String ACCOUNT_ADMIN_LOGIN = "jirabitbucketconnector";
    private static final String ACCOUNT_ADMIN_PASSWORD = PasswordUtil.getPassword("jirabitbucketconnector");

    private static ServiceRemoteRestpoint serviceRemoteRestpoint;
    private BitbucketIntegratedApplicationsPage bitbucketIntegratedApplicationsPage;

    @Override
    protected Class<BitBucketConfigureOrganizationsPage> getConfigureOrganizationsPageClass()
    {
        return BitBucketConfigureOrganizationsPage.class;
    }


    @BeforeClass
    public static void initializeServiceREST()
    {
        AuthProvider basicAuthProvider = new BasicAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                                                                   ACCOUNT_ADMIN_LOGIN,
                                                                   ACCOUNT_ADMIN_PASSWORD);
        serviceRemoteRestpoint = new ServiceRemoteRestpoint(basicAuthProvider.provideRequestor());
    }

    @BeforeMethod
    public void removeExistingPostCommitHooks()
    {
        Set<Integer> extractedBitbucketServiceIds = extractBitbucketServiceIdsToRemove();
        for (int extractedBitbucketServiceId : extractedBitbucketServiceIds)
        {
            serviceRemoteRestpoint.deleteService("jirabitbucketconnector", "public-hg-repo", extractedBitbucketServiceId);
        }
    }

    private void loginToBitbucketAndSetJiraOAuthCredentials()
    {
        jira.getTester().gotoUrl(BitbucketLoginPage.LOGIN_PAGE);
        jira.getPageBinder().bind(BitbucketLoginPage.class).doLogin();

        jira.getTester().gotoUrl(BitbucketIntegratedApplicationsPage.PAGE_URL);
        bitbucketIntegratedApplicationsPage = jira.getPageBinder().bind(BitbucketIntegratedApplicationsPage.class);

        BitbucketIntegratedApplicationsPage.OAuthCredentials oauthCredentials =
                bitbucketIntegratedApplicationsPage.addConsumer();

        BitbucketOAuthConfigPage oauthConfigPage = jira.getPageBinder().navigateToAndBind(BitbucketOAuthConfigPage.class);
        oauthConfigPage.setCredentials(oauthCredentials.oauthKey, oauthCredentials.oauthSecret);

        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + configureOrganizations.getUrl());
    }

    private void deleteAllOrganizationsAndRemoveOAuthConsumer()
    {
        jira.goTo(getConfigureOrganizationsPageClass());
        configureOrganizations.deleteAllOrganizations();

        jira.getTester().gotoUrl(BitbucketIntegratedApplicationsPage.PAGE_URL);
        bitbucketIntegratedApplicationsPage.removeLastAdddedConsumer();
    }

    @Test
    public void addOrganization()
    {
        loginToBitbucketAndSetJiraOAuthCredentials();
        BaseConfigureOrganizationsPage organizationsPage =
                configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, false);

        PageElement repositoriesTable = organizationsPage.getOrganizations().get(0).getRepositoriesTable();
        // first row is header row, than repos ...
        assertThat(repositoriesTable.findAll(By.tagName("tr")).size()).isGreaterThan(2);

        // check add user extension
        jira.visit(JiraAddUserPage.class).checkPanelPresented();

        deleteAllOrganizationsAndRemoveOAuthConsumer();
    }

    @Test
    public void addOrganizationAutoSync()
    {
        loginToBitbucketAndSetJiraOAuthCredentials();
        BaseConfigureOrganizationsPage organizationsPage =
                configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, true);
        PageElement repositoriesTable = organizationsPage.getOrganizations().get(0).getRepositoriesTable();
        // first row is header row, than repos ...
        assertThat(repositoriesTable.findAll(By.tagName("tr")).size()).isGreaterThan(2);

        deleteAllOrganizationsAndRemoveOAuthConsumer();
    }

    @Test
    public void addUrlThatDoesNotExist()
    {
        configureOrganizations.addOrganizationFailingStep1(TEST_NOT_EXISTING_URL);

        String errorMessage = configureOrganizations.getErrorStatusMessage();
        assertThat(errorMessage).contains("is incorrect or the server is not responding");

        configureOrganizations.clearForm();
    }

    @Test
    public void testPostCommitHookAdded() throws Exception
    {
        loginToBitbucketAndSetJiraOAuthCredentials();
        String servicesConfig;
        String baseUrl = jira.getProductInstance().getBaseUrl();

        // add account
        configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, true);
        // check that it created postcommit hook
        String syncUrl = baseUrl + "/rest/bitbucket/1.0/repository/";
        String bitbucketServiceConfigUrl = "https://bitbucket.org/!api/1.0/repositories/jirabitbucketconnector/public-hg-repo/services";
        servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, ACCOUNT_ADMIN_LOGIN, ACCOUNT_ADMIN_PASSWORD);
        assertThat(servicesConfig).contains(syncUrl);
        // delete repository
        configureOrganizations.deleteAllOrganizations();
        // check that postcommit hook is removed
        servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, ACCOUNT_ADMIN_LOGIN, ACCOUNT_ADMIN_PASSWORD);
        assertThat(servicesConfig).doesNotContain(syncUrl);

        deleteAllOrganizationsAndRemoveOAuthConsumer();
    }

    private String getBitbucketServices(String url, String username, String password) throws Exception
    {
        HttpClient httpClient = new HttpClient();
        HttpMethod method = new GetMethod(url);

        AuthScope authScope = new AuthScope(method.getURI().getHost(), AuthScope.ANY_PORT, null, AuthScope.ANY_SCHEME);
        httpClient.getParams().setAuthenticationPreemptive(true);
        httpClient.getState().setCredentials(authScope, new UsernamePasswordCredentials(username, password));

        httpClient.executeMethod(method);
        return method.getResponseBodyAsString();
    }

    @Test
    public void addRepoCommitsAppearOnIssues()
    {
        loginToBitbucketAndSetJiraOAuthCredentials();
        configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, true);

        assertThat(getCommitsForIssue("QA-2", 1)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
        assertThat(getCommitsForIssue("QA-3", 2)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");

        deleteAllOrganizationsAndRemoveOAuthConsumer();
    }

    @Test
    public void testCommitStatistics()
    {
        loginToBitbucketAndSetJiraOAuthCredentials();
        configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, true);

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

        deleteAllOrganizationsAndRemoveOAuthConsumer();
    }


    private static Set<Integer> extractBitbucketServiceIdsToRemove()
    {
        List<BitbucketServiceEnvelope> bitbucketServices =
                serviceRemoteRestpoint.getAllServices("jirabitbucketconnector", "public-hg-repo");

        Set<Integer> extractedServiceIds = new LinkedHashSet<Integer>();

        for (BitbucketServiceEnvelope bitbucketServiceEnvelope : bitbucketServices)
        {
            int bitbucketServiceId = bitbucketServiceEnvelope.getId();

            for (BitbucketServiceField bitbucketServiceField : bitbucketServiceEnvelope.getService().getFields())
            {
                if (bitbucketServiceField.getName().equals("URL"))
                {
                    String serviceURL = bitbucketServiceField.getValue();

                    if (serviceURL.contains(jira.getProductInstance().getBaseUrl())) {
                        extractedServiceIds.add(bitbucketServiceId);
                    }
                }
            }
        }

        return extractedServiceIds;
    }
}
