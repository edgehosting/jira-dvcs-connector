package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import org.openqa.selenium.By;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;

/**
 * Represents the page to link repositories to projects.
 */
public class BitBucketConfigureOrganizationsPage extends BaseConfigureOrganizationsPage
{
    @ElementBy(id = "oauthClientId")
    PageElement oauthKeyInput;

    @ElementBy(id = "oauthSecret")
    PageElement oauthSecretInput;

    @ElementBy(id = "atlassian-token")
    PageElement atlassianTokenMeta;

    @ElementBy(id = "oauthBbClientId")
    private PageElement oauthBbClientId;

    @ElementBy(id = "oauthBbSecret")
    private PageElement oauthBbSecret;

    @Override
    public BitBucketConfigureOrganizationsPage addOrganizationSuccessfully(String organizationAccount, OAuthCredentials oAuthCredentials, boolean autoSync, String username, String password)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(0));

        if (!autoSync)
        {
            autoLinkNewRepos.click();
        }

        fillInDetailsAndSubmit(organizationAccount, oAuthCredentials);

        if (isFormOpen().by(15, SECONDS))
        {
            // if form still open, assume it hits the weird clear text error, where some or all fields are cleared after filled in
            //  just retry the filling and submit again.
            fillInDetailsAndSubmit(organizationAccount, oAuthCredentials);
        }

        Poller.waitUntilFalse(atlassianTokenMeta.timed().isPresent());
        pageBinder.bind(BitbucketGrandOAuthAccessPage.class).grantAccess();
        Poller.waitUntilTrue(linkRepositoryButton.timed().isPresent());

        if (autoSync)
        {
            JiraPageUtils.checkSyncProcessSuccess(pageBinder);
        }

        return this;
    }

    private void fillInDetailsAndSubmit(final String organizationAccount, final OAuthCredentials oAuthCredentials)
    {
        organization.clear().type(organizationAccount);

        oauthBbClientId.clear().type(oAuthCredentials.key);
        oauthBbSecret.clear().type(oAuthCredentials.secret);

        addOrgButton.click();
    }

    /**
     * Links a public repository to the given JIRA project.
     *
     * @param url        The url to the bitucket public repo
     * @return BitBucketConfigureOrganizationsPage
     */
    @Override
    public BitBucketConfigureOrganizationsPage addOrganizationFailingStep1(String url)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        organization.clear().type(url);
        addOrgButton.click();

        TimedCondition hasText = messageBarDiv.find(By.tagName("strong")).timed().hasText("Error!");

        Poller.waitUntil("Expected Error message while connecting repository", hasText, is(true), Poller.by(30000));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseConfigureOrganizationsPage addOrganizationFailingOAuth(String username, String password)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        organization.clear().type("https://bitbucket.org/someaccount");
        addOrgButton.click();
		Poller.waitUntilTrue(
				"Expected form for bitbucket repository admin login/password!",
				Conditions.and(oauthKeyInput.timed().isVisible(),
						oauthSecretInput.timed().isVisible()));

        return this;
    }
}
