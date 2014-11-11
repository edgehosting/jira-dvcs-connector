package it.com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.BaseConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import org.testng.annotations.Test;

import javax.ws.rs.core.UriBuilder;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for opening the "Add New Account" dialog from URL
 *
 * ie ConfigureDvcsOrganizations!default.jspa?source=devtools&selectHost=github#expand
 */
public class TestAddNewAccountDialogFromURL extends BaseOrganizationTest
{
    private AutoOpenDialogConfigureOrganizationsPage commonConfigureOrganizationPage;

    @Test
    public void testAutoOpenDialogForBitbucket()
    {
        goToConfigureOrganizationPage(true, "bitbucket");

        waitUntilTrue(commonConfigureOrganizationPage.isFormOpen());
        waitUntil(commonConfigureOrganizationPage.getDvcsTypeSelectValue(), equalTo("bitbucket"));
        waitUntil(commonConfigureOrganizationPage.getFormAction(), containsString("AddBitbucketOrganization.jspa"));
    }

    @Test
    public void testAutoOpenDialogForGithub()
    {
        goToConfigureOrganizationPage(true, "github");

        waitUntilTrue(commonConfigureOrganizationPage.isFormOpen());
        waitUntil(commonConfigureOrganizationPage.getDvcsTypeSelectValue(), equalTo("github"));
        waitUntil(commonConfigureOrganizationPage.getFormAction(), containsString("AddGithubOrganization.jspa"));
    }

    @Test
    public void testAutoOpenDialogForGithubE()
    {
        goToConfigureOrganizationPage(true, "githube");

        waitUntilTrue(commonConfigureOrganizationPage.isFormOpen());
        waitUntil(commonConfigureOrganizationPage.getDvcsTypeSelectValue(), equalTo("githube"));
        waitUntil(commonConfigureOrganizationPage.getFormAction(), containsString("AddGithubEnterpriseOrganization.jspa"));
    }

    @Test
    public void testAutoOpenDialogDefaultToBitbucket()
    {
        String[] selectHosts = {"gibberish", null};
        for (String selectHost : selectHosts)
        {
            goToConfigureOrganizationPage(true, selectHost);
            waitUntilTrue(commonConfigureOrganizationPage.isFormOpen());
            waitUntil(commonConfigureOrganizationPage.getDvcsTypeSelectValue(), equalTo("bitbucket"));
            waitUntil(commonConfigureOrganizationPage.getFormAction(), containsString("AddBitbucketOrganization.jspa"));
        }
    }

    @Test
    public void testDialogDoesntOpenWithoutHashExpand()
    {
        goToConfigureOrganizationPage(false, "doesn't matter");
        waitUntilFalse(commonConfigureOrganizationPage.isFormOpen());
    }

    @Override
    protected Class getConfigureOrganizationsPageClass()
    {
        return AutoOpenDialogConfigureOrganizationsPage.class;
    }

    private void goToConfigureOrganizationPage(Boolean expand, String selectHost)
    {
        commonConfigureOrganizationPage = jira.getPageBinder().navigateToAndBind(
                AutoOpenDialogConfigureOrganizationsPage.class,
                expand,
                selectHost);
    }

    public static class AutoOpenDialogConfigureOrganizationsPage extends BaseConfigureOrganizationsPage
    {
        // Tests do not use any of these
        @Override
        public BaseConfigureOrganizationsPage addOrganizationFailingStep1(String url) {return null;}
        @Override
        public BaseConfigureOrganizationsPage addOrganizationFailingOAuth(String username, String password) {return null;}
        @Override
        public BaseConfigureOrganizationsPage addOrganizationSuccessfully(String organizationAccount, OAuthCredentials oAuthCredentials, boolean autosync, String username, String password) {return null;}

        private final String selectHost;
        private final Boolean expand;

        public AutoOpenDialogConfigureOrganizationsPage()
        {
            super();
            this.selectHost = null;
            this.expand = false;
        }

        public AutoOpenDialogConfigureOrganizationsPage(Boolean expand, String selectHost)
        {
            super();
            this.expand = expand;
            this.selectHost = selectHost;
        }

        @Override
        public String getUrl()
        {
            UriBuilder uriBuilder = UriBuilder.fromUri("/secure/admin/ConfigureDvcsOrganizations!default.jspa");
            if (selectHost != null)
            {
                uriBuilder.queryParam("selectHost", selectHost);
            }
            if (expand)
            {
                uriBuilder.fragment("expand");
            }
            return uriBuilder.build().toString();
        }
    }
}
