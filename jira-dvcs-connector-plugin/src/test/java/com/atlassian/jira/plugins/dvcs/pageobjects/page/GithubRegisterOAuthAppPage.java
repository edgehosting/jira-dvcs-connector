package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubConfirmPasswordPage;

import java.util.List;

import org.openqa.selenium.By;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

/**
 *
 */
public class GithubRegisterOAuthAppPage implements Page
{
    public static final String PAGE_PATH = "/settings/applications/new";
    public static final String PAGE_URL = "https://github.com" + PAGE_PATH;

    @ElementBy(name = "oauth_application[name]")
    PageElement oauthApplicationName;

    @ElementBy(name = "oauth_application[url]")
    PageElement oauthApplicationUrl;

    @ElementBy(name = "oauth_application[callback_url]")
    PageElement oauthApplicationCallbackUrl;

    @ElementBy(cssSelector = ".new_oauth_application button")
    PageElement submitButton;

    @ElementBy(tagName = "body")
    PageElement bodyElm;

    @ElementBy(cssSelector = ".keys")
    PageElement secrets;

    PageElement clientId;
    PageElement clientSecret;

    @Override
    public String getUrl()
    {
        return PAGE_URL;
    }

    public void registerApp(String appName, String appUrl, String appCallbackUrl)
    {
        oauthApplicationName.type(appName);
        oauthApplicationUrl.type(appUrl);
        oauthApplicationCallbackUrl.type(appCallbackUrl);
        submitButton.click();

        Poller.waitUntilTrue(secrets.timed().isVisible());

        List<PageElement> allSecretsElements = secrets.findAll(By.tagName("dd"));
        clientId = allSecretsElements.get(0);
        clientSecret = allSecretsElements.get(1);
    }

    public void deleteOAuthApp(JiraTestedProduct jira, String password)
    {

        PageElement deleteForm = null;
        deleteForm = getDeleteForm(deleteForm);
        List<PageElement> allFormLinks = deleteForm.findAll(By.tagName("a"));
		allFormLinks.get(allFormLinks.size() - 1).click();
        deleteForm = getPopupDeleteForm(deleteForm);
        deleteForm.find(By.tagName("button")).click();

        if (requiresPasswordConfirmation(jira))
        {
            GithubConfirmPasswordPage confirmPasswordPage = jira.getPageBinder().bind(GithubConfirmPasswordPage.class);
            confirmPasswordPage.confirmPassword(password);
        }
    }

    private boolean requiresPasswordConfirmation(JiraTestedProduct jira)
    {
        // if access has been granted before browser will
        // redirect immediately back to jira
        String currentUrl = jira.getTester().getDriver().getCurrentUrl();
        return currentUrl.contains("/session/confirm");
    }

    private PageElement getDeleteForm(PageElement deleteForm)
	{
		List<PageElement> allForms = bodyElm.findAll(By.tagName("form"));
        for (PageElement form : allForms)
		{
			if (form.getAttribute("action").contains("settings/applications")) {
				deleteForm = form;
				break;
			}
		}
		return deleteForm;
	}

	private PageElement getPopupDeleteForm(PageElement deleteForm)
	{
		List<PageElement> allForms = bodyElm.findAll(By.tagName("form"));
		for (PageElement form : allForms)
		{
			if (form.getAttribute("action").contains("settings/applications")) {
				deleteForm = form;
			}
		}
		Poller.waitUntilTrue(deleteForm.find(By.tagName("button")).timed().isVisible());
		return deleteForm;
	}

	public PageElement getClientId()
	{
		return clientId;
	}

	public PageElement getClientSecret()
	{
		return clientSecret;
	}



}
