package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import java.util.List;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.TimedCondition;

public class GithubRegisteredOAuthAppsPage extends AbstractJiraPage
{
    public static final String PAGE_URL = "https://github.com/settings/applications";

    private String oauthAppUrl;

    @Override
    public String getUrl()
    {
        return PAGE_URL;
    }

    @Override
    public TimedCondition isAt() {
        return body.timed().isVisible();
    }

    public void parseClientIdAndSecret(String appName)
    {
    	List<PageElement> applications = body.findAll(By.cssSelector("li.linked-item a"));
    	PageElement lastApplication = applications.get(applications.size() - 1);
    	oauthAppUrl = lastApplication.getAttribute("href");
    }

    public String getOauthAppUrl()
    {
        return oauthAppUrl;
    }
}
