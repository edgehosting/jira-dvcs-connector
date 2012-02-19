package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

/**
 *
 */
public class GithubRegisterOAuthAppPage implements Page
{
    public static final String PAGE_URL = "https://github.com/account/applications/new";

    @ElementBy(name = "oauth_application[name]")
    PageElement oauthApplicationName;

    @ElementBy(name = "oauth_application[url]")
    PageElement oauthApplicationUrl;

    @ElementBy(name = "oauth_application[callback_url]")
    PageElement oauthApplicationCallbackUrl;

    @ElementBy(tagName = "button")
    PageElement submitButton;

    @ElementBy(tagName = "body")
    PageElement bodyElm;

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
    }

    public void deleteOAuthApp()
    {
        PageElement showPopupBtn = bodyElm.find(By.className("minibutton"));
        showPopupBtn.click();
        PageElement confirmBtn = bodyElm.find(By.id("facebox")).find(By.className("danger"));
        confirmBtn.click();
    }
}
