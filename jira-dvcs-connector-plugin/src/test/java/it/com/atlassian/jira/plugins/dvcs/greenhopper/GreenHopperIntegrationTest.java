package it.com.atlassian.jira.plugins.dvcs.greenhopper;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitBucketConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketIntegratedApplicationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthConfigPage;

import org.junit.Test;
import it.com.atlassian.jira.plugins.dvcs.BitBucketBaseOrgTest;

/**
 * @author Martin Skurla
 */
public class GreenHopperIntegrationTest extends BitBucketBaseOrgTest<BitBucketConfigureOrganizationsPage>
{
    private static final String TEST_ORGANIZATION = "jirabitbucketconnector";

    private BitbucketIntegratedApplicationsPage bitbucketIntegratedApplicationsPage;


    @Test
    public void greenHopperIntegration_ShouldAddDvcsCommitsTab()
    {
        loginToBitbucketAndSetJiraOAuthCredentials();

        configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, true);

        GreenHopperBoardPage greenHopperBoardPage = jira.getPageBinder().navigateToAndBind(GreenHopperBoardPage.class);
        greenHopperBoardPage.goToQABoardPlan();
        greenHopperBoardPage.assertCommitsAppearOnIssue("QA-1", 5);

        removeOAuthConsumer();
    }

    @Override
    protected Class<BitBucketConfigureOrganizationsPage> getConfigureOrganizationsPageClass()
    {
        return BitBucketConfigureOrganizationsPage.class;
    }


    private void loginToBitbucketAndSetJiraOAuthCredentials() //TODO @Before vs not needed for every test method
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

    private void removeOAuthConsumer() //TODO @After vs not needed for every method
    {
        jira.getTester().gotoUrl(BitbucketIntegratedApplicationsPage.PAGE_URL);
        bitbucketIntegratedApplicationsPage.removeLastAdddedConsumer();
    }
}
