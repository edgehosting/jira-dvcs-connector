package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import javax.inject.Inject;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

/**
 * Represents the JIRA view issue page
 */
public class JiraViewIssuePage implements Page
{
    @Inject
    PageBinder pageBinder;
    
    @ElementBy(id="bitbucket-commits-tabpanel")
    PageElement trigger;

    private final String issueKey;

    public JiraViewIssuePage(String issueKey)
    {
        this.issueKey = issueKey;
    }

    public String getUrl()
    {
        return "/browse/" + issueKey;
    }

    public String getBitBucketPanelUrl()
    {
    	Poller.waitUntilTrue(trigger.timed().isVisible());
    	String attributeHref = trigger.getAttribute("href");
    	String url = null;
    	if(attributeHref != null) {
    		url = attributeHref.replaceAll("/jira", "");
    	}
    	return url;
    }
}
