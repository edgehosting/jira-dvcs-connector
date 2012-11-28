package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import java.util.List;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

public class GithubRegisteredOAuthAppsPage implements Page
{
    public static final String PAGE_URL = "https://github.com/settings/applications";

    @ElementBy(tagName = "body")
    PageElement pageBodyElm;

    private String oauthAppUrl;

    @Override
    public String getUrl()
    {
        return PAGE_URL;
    }

    public void parseClientIdAndSecret(String appName)
    {
    	
    	List<PageElement> applications = pageBodyElm.findAll(By.cssSelector("li.linked-item a"));
    	PageElement lastApplication = applications.get(applications.size() - 1);
    	oauthAppUrl = lastApplication.getAttribute("href");

    }

    public String getOauthAppUrl()
    {
        return oauthAppUrl;
    }
}
