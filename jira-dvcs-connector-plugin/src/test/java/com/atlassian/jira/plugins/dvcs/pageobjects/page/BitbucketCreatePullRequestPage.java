package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import javax.inject.Inject;

import static com.atlassian.pageobjects.elements.query.Poller.by;
import static org.hamcrest.core.Is.is;

/**
 * @author Miroslav Stencel mstencel@atlassian.com
 */
public class BitbucketCreatePullRequestPage implements Page
{    
    public static final String PAGE_URL = "https://bitbucket.org/jirabitbucketconnector/public-hg-repo/pull-request/new";

    @ElementBy(id = "id_title")
    private PageElement titleElement;

    @ElementBy(id = "id_description")
    private PageElement descriptionElement;
   
    @ElementBy(id = "submitPrButton")
    private PageElement submitButton;
    
    @Inject
    private PageElementFinder elementFinder;
    
    @Inject
    WebDriver webDriver;

    private String url;

    public static String getUrl(String owner, String slug)
    {
        return String.format("https://bitbucket.org/%s/%s/pull-request/new", owner, slug);
    }

    public BitbucketCreatePullRequestPage()
    {

    }

    public BitbucketCreatePullRequestPage(final String url)
    {
        this.url = url;
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    public String createPullRequest(String title, String description, String branch, String base, String toRepository)
    {
        PageElement pullFrom = elementFinder.find(By.xpath("//div[@id='pull-from']//div[@class='branch-field-container']//*[contains(concat(' ', @class , ' '),' chzn-single ')]"));
        if (branch != null && !branch.equals(pullFrom.getText()))
        {
            pullFrom.click();
            pullFrom.find(By.xpath("//li[text() = '" + branch + "']")).click();
        }
        
        PageElement pullInto = elementFinder.find(By.xpath(String.format("//div[@id='pull-into']//div[@class='branch-field-container']//span[contains(@data-value,'%s')]",toRepository)));
        if (base != null && !base.equals(pullInto.getText()))
        {
            pullInto.click();
            pullInto.find(By.xpath("//li[text() = '" + base + "']")).click();
        }
        if (title != null)
        {
            titleElement.clear().type(title);
        }
        if (description != null)
        {
            descriptionElement.clear().type(description);
        }
        
        submitButton.click();
        try
        {
            Poller.waitUntil(submitButton.timed().isPresent(), is(false), by(15000));
        }  catch (AssertionError e)
        {
            // we will continue in hope that Bitbucket did its job
        }
        return webDriver.getCurrentUrl();
    }
}
