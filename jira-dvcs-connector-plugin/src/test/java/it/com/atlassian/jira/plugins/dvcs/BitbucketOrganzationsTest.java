package it.com.atlassian.jira.plugins.dvcs;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;

import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BaseConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitBucketConfigureOrganizationsPage;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.pageobjects.elements.PageElement;

import com.atlassian.jira.plugins.dvcs.util.HttpSenderUtils;

import static com.atlassian.jira.plugins.dvcs.pageobjects.CommitMessageMatcher.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Test to verify behaviour when syncing bitbucket repository..
 */
public class BitbucketOrganzationsTest extends BitBucketBaseOrgTest
{
	private static final String TEST_URL = "https://bitbucket.org";
	private static final String TEST_NOT_EXISTING_URL = "https://privatebitbucket.org/someaccount";
	private static final String ACCOUNT_ADMIN_LOGIN = "jirabitbucketconnector";
	private static final String ACCOUNT_ADMIN_PASSWORD = "jirabitbucketconnector1";

	@Override
	protected Class<BitBucketConfigureOrganizationsPage> getPageClass()
	{
		return BitBucketConfigureOrganizationsPage.class;
	}


    @Before
    public void removeExistingPostCommitHooks()
    {
        Set<String> extractedBitbucketServiceIds = extractBitbucketServiceIds();

        for (String extractedBitbucketServiceId : extractedBitbucketServiceIds)
        {
            removePostCommitHook(extractedBitbucketServiceId);
        }
    }

	@Test
	public void addOrganization()
	{
		BaseConfigureOrganizationsPage organizationsPage = configureOrganizations.addOrganizationSuccessfully(TEST_URL,
				false);
		PageElement repositoriesTable = organizationsPage.getOrganizations().get(0).getRepositoriesTable();
		// first row is header row, than repos ...
		Assert.assertTrue(repositoriesTable.findAll(By.tagName("tr")).size() > 2);
	}

	@Test
	public void addOrganizationAutoSync()
	{
		BaseConfigureOrganizationsPage organizationsPage = configureOrganizations.addOrganizationSuccessfully(TEST_URL,
				true);
		PageElement repositoriesTable = organizationsPage.getOrganizations().get(0).getRepositoriesTable();
		// first row is header row, than repos ...
		Assert.assertTrue(repositoriesTable.findAll(By.tagName("tr")).size() > 2);
	}

	@Test
	public void addUrlThatDoesNotExist()
	{
		configureOrganizations.addOrganizationFailingStep1(TEST_NOT_EXISTING_URL);

		String errorMessage = configureOrganizations.getErrorStatusMessage();
		assertThat(errorMessage, containsString("is incorrect or the server is not responding."));

		configureOrganizations.clearForm();
	}

	@Test
	public void testPostCommitHookAdded() throws Exception
	{
		String servicesConfig;
		String baseUrl = jira.getProductInstance().getBaseUrl();

		// add account
		configureOrganizations.addOrganizationSuccessfully(TEST_URL, true);
		// check that it created postcommit hook
		String syncUrl = baseUrl + "/rest/bitbucket/1.0/repository/";
		String bitbucketServiceConfigUrl = "https://bitbucket.org/!api/1.0/repositories/jirabitbucketconnector/public-hg-repo/services";
		servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, ACCOUNT_ADMIN_LOGIN, ACCOUNT_ADMIN_PASSWORD);
		assertThat(servicesConfig, containsString(syncUrl));
		// delete repository
		configureOrganizations.deleteAllOrganizations();
		// check that postcommit hook is removed
		servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, ACCOUNT_ADMIN_LOGIN, ACCOUNT_ADMIN_PASSWORD);
		assertThat(servicesConfig, not(containsString(syncUrl)));
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
		configureOrganizations.addOrganizationSuccessfully(TEST_URL, true);

		assertThat(getCommitsForIssue("QA-2"),
				hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
		assertThat(getCommitsForIssue("QA-3"),
				hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
	}

	@Test
	public void testCommitStatistics()
	{
		configureOrganizations.addOrganizationSuccessfully(TEST_URL, true);

		// QA-2
		List<BitBucketCommitEntry> commitMessages = getCommitsForIssue("QA-2");
		Assert.assertEquals("Expected 1 commit", 1, commitMessages.size());
		BitBucketCommitEntry commitMessage = commitMessages.get(0);
		List<PageElement> statistics = commitMessage.getStatistics();
		Assert.assertEquals("Expected 1 statistic", 1, statistics.size());
		Assert.assertTrue("Expected Added", commitMessage.isAdded(statistics.get(0)));

		// QA-3
		commitMessages = getCommitsForIssue("QA-3");
		Assert.assertEquals("Expected 2 commits", 2, commitMessages.size());
		// commit 1
		commitMessage = commitMessages.get(0);
		statistics = commitMessage.getStatistics();
		Assert.assertEquals("Expected 1 statistic", 1, statistics.size());
		Assert.assertTrue("Expected Added", commitMessage.isAdded(statistics.get(0)));
		// commit 2
		commitMessage = commitMessages.get(1);
		statistics = commitMessage.getStatistics();
		Assert.assertEquals("Expected 1 statistic", 1, statistics.size());
		Assert.assertEquals("Expected Additions: 1", commitMessage.getAdditions(statistics.get(0)), "+3");
		Assert.assertEquals("Expected Deletions: -", commitMessage.getDeletions(statistics.get(0)), "-");
	}


    private static Set<String> extractBitbucketServiceIds()
    {
        String listServicesResponseString;

        try
        {
            listServicesResponseString = HttpSenderUtils.sendGetHttpRequest(
                    "https://api.bitbucket.org/1.0/repositories/jirabitbucketconnector/public-hg-repo/services/",
                    ACCOUNT_ADMIN_LOGIN,
                    ACCOUNT_ADMIN_PASSWORD);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot extract BitBucket service IDs !", e);
        }

        Set<String> extractedServiceIds = new LinkedHashSet<String>();

        try {
            JSONArray jsonArray = new JSONArray(listServicesResponseString);
            for (int i = 0; i < jsonArray.length(); i++)
            {
                JSONObject data = (JSONObject) jsonArray.get(i);

                String bitbucketServiceId = data.getString("id");
                JSONObject serviceObject = data.getJSONObject("service");
                JSONArray  fieldsArray = serviceObject.getJSONArray("fields");

                for (int j = 0; j < fieldsArray.length(); j++) {
                    JSONObject fieldObject = fieldsArray.getJSONObject(j);

                    if (fieldObject.getString("name").equals("URL")) {
                        String serviceURL = fieldObject.getString("value");

                        if (serviceURL.contains(jira.getProductInstance().getBaseUrl())) {
                            extractedServiceIds.add(bitbucketServiceId);
                        }
                    }
                }
            }
        }
        catch (JSONException e)
        {
            throw new IllegalStateException("Cannot parse JSON !", e);
        }

        return extractedServiceIds;
    }

    private static void removePostCommitHook(String serviceId) {
        String finalBitbucketUrl = String.format(
                "https://api.bitbucket.org/1.0/repositories/jirabitbucketconnector/public-hg-repo/services/%s",
                serviceId);
        try
        {
            HttpSenderUtils.sendDeleteHttpRequest(finalBitbucketUrl,
                                  ACCOUNT_ADMIN_LOGIN,
                                  ACCOUNT_ADMIN_PASSWORD);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot send DELETE Http request !", e);
        }
    }
}
