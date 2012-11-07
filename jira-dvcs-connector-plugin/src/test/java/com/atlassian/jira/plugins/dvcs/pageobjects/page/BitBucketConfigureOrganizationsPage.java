package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import javax.inject.Inject;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;

/**
 * Represents the page to link repositories to projects.
 */
public class BitBucketConfigureOrganizationsPage extends BaseConfigureOrganizationsPage
{
    @Inject
    PageBinder pageBinder;

    @ElementBy(id = "oauthClientId")
    PageElement oauthKeyInput;

    @ElementBy(id = "oauthSecret")
    PageElement oauthSecretInput;

    @ElementBy(id = "atlassian-token")
    PageElement atlassianTokenMeta;


    @Override
    public BitBucketConfigureOrganizationsPage addOrganizationSuccessfully(String url, String organizationAccount,
            boolean autoSync)
    {
        linkRepositoryButton.click();
        waitFormBecomeVisible();

        dvcsTypeSelect.select(dvcsTypeSelect.getAllOptions().get(0));

        organization.clear().type(organizationAccount);

        if (!autoSync) {
            autoLinkNewRepos.click();
        }

        addOrgButton.click();

        Poller.waitUntilFalse(atlassianTokenMeta.timed().isPresent());
        pageBinder.bind(BitbucketGrandOAuthAccessPage.class).grantAccess();

        if (autoSync) {
            checkSyncProcessSuccess();
        }

        return this;
    }

    /**
     * Links a public repository to the given JIRA project.
     *
     * @param projectKey The JIRA project key
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

        Poller.waitUntilTrue("Expected Error message while connecting repository", messageBarDiv.find(By.tagName("strong")).timed().hasText("Error!"));

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseConfigureOrganizationsPage addRepoToProjectFailingStep2(String url)
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

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseConfigureOrganizationsPage addRepoToProjectFailingPostcommitService(String url)
    {
        return this;
    }
}
