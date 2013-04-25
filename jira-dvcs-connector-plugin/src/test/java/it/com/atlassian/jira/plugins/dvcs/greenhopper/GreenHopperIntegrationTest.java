package it.com.atlassian.jira.plugins.dvcs.greenhopper;

import it.com.atlassian.jira.plugins.dvcs.BaseOrganizationTest;

import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitBucketConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketIntegratedApplicationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;

/**
 * @author Martin Skurla
 */
public class GreenHopperIntegrationTest extends BaseOrganizationTest<BitBucketConfigureOrganizationsPage>
{
    private static final String TEST_ORGANIZATION = "jirabitbucketconnector";

    private BitbucketIntegratedApplicationsPage bitbucketIntegratedApplicationsPage;


    @Test
    public void greenHopperIntegration_ShouldAddDvcsCommitsTab()
    {
        OAuthCredentials oAuthCredentials = loginToBitbucketAndSetJiraOAuthCredentials();

        configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, oAuthCredentials, true);

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

    private OAuthCredentials loginToBitbucketAndSetJiraOAuthCredentials()
    {
        jira.getTester().gotoUrl(BitbucketLoginPage.LOGIN_PAGE);
        jira.getPageBinder().bind(BitbucketLoginPage.class).doLogin();

        jira.getTester().gotoUrl(BitbucketIntegratedApplicationsPage.PAGE_URL);
        bitbucketIntegratedApplicationsPage = jira.getPageBinder().bind(BitbucketIntegratedApplicationsPage.class);

        OAuthCredentials oAuthCredentials =
                bitbucketIntegratedApplicationsPage.addConsumer();

//        BitbucketOAuthConfigPage oauthConfigPage = jira.getPageBinder().navigateToAndBind(BitbucketOAuthConfigPage.class);
//        oauthConfigPage.setCredentials(oauthCredentials.oauthKey, oauthCredentials.oauthSecret);

        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + configureOrganizations.getUrl());
        return oAuthCredentials;
    }

    private void removeOAuthConsumer()
    {
        jira.getTester().gotoUrl(BitbucketIntegratedApplicationsPage.PAGE_URL);
        bitbucketIntegratedApplicationsPage.removeLastAdddedConsumer();
    }
}
