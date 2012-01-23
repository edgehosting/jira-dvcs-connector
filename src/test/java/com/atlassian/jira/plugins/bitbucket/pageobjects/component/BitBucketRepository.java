package com.atlassian.jira.plugins.bitbucket.pageobjects.component;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.AtlassianWebDriver;
import org.openqa.selenium.By;

import javax.inject.Inject;

/**
 * Represents a repository that is linked to a project (a table row of <tt>BitBucketConfigureRepositoriesPage</tt>)
 */
public class BitBucketRepository
{
    private final PageElement row;

    @Inject
    AtlassianWebDriver driver;

    @Inject
    PageElementFinder elementFinder;

    private String projectKey;

    public BitBucketRepository(PageElement row, String projectKey)
    {
        this.row = row;
        this.projectKey = projectKey;
    }

    /**
     * The url of this repo
     *
     * @return Url
     */
    public String getUrl()
    {
        return row.find(By.tagName("a")).getAttribute("href");
    }

    /**
     * The projec key
     *
     * @return Key
     */
    public String getProjectKey()
    {
        return projectKey;
    }

    /**
     * Deletes this repository from the list
     */
    public void delete()
    {
        // disable confirm popup
        driver.executeScript("window.confirm = function(){ return true; }");

        // add marker to wait for post complete
        driver.executeScript("document.getElementById('Submit').className = '_posting'");
        row.find(By.linkText("Delete")).click();

        //wait until marker is gone.
        Poller.waitUntilFalse(elementFinder.find(By.id("Submit")).timed().hasClass("_posting"));
    }
}
