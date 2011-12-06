package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

import java.util.List;

/**
 *
 */
public class GithubRegisteredOAuthAppsPage implements Page
{
    public static final String PAGE_URL = "https://github.com/account/applications";

    @ElementBy(tagName = "body")
    PageElement pageBodyElm;

    private String clientID;
    private String clientSecret;
    private String oauthAppUrl;

    @Override
    public String getUrl()
    {
        return PAGE_URL;
    }

    public void parseClientIdAndSecret(String appName)
    {
        List<PageElement> appRecords = pageBodyElm.findAll(By.className("info"));
        for (PageElement appRecordDiv : appRecords)
        {
            if (!appRecordDiv.find(By.linkText(appName)).isPresent())
            {
                continue;
            }
            oauthAppUrl = "https://github.com" + pageBodyElm.find(By.linkText(appName)).getAttribute("href");

            PageElement statisticInfoDiv = appRecordDiv.find(By.className("body"));
            final String clientIdPrefix = "Client ID:";
            final String clientSecretPrefix = "Secret:";
            List<PageElement> elements = statisticInfoDiv.findAll(By.tagName("li"));
            for (PageElement elm : elements)
            {
                String elmText = elm.getText();
                if (elmText.contains(clientIdPrefix))
                {
                    clientID = elmText.substring(elmText.indexOf(clientIdPrefix) + clientIdPrefix.length()).trim();
                }
                if (elmText.contains(clientSecretPrefix))
                {
                    clientSecret = elmText.substring(elmText.indexOf(clientSecretPrefix) + clientSecretPrefix.length()).trim();
                }
            }
        }
    }

    public String getClientID()
    {
        return clientID;
    }

    public String getClientSecret()
    {
        return clientSecret;
    }

    public String getOauthAppUrl()
    {
        return oauthAppUrl;
    }
}
