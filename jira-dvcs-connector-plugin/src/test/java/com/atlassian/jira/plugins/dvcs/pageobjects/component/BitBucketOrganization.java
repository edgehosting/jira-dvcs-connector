package com.atlassian.jira.plugins.dvcs.pageobjects.component;

import static com.atlassian.pageobjects.elements.query.Poller.by;
import static org.hamcrest.Matchers.is;

import javax.inject.Inject;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.AtlassianWebDriver;

public class BitBucketOrganization
{
    private final PageElement row;

    @Inject
    AtlassianWebDriver driver;

    @Inject
    PageElementFinder elementFinder;
    
    @ElementBy(tagName = "table")
    PageElement repositoriesTable;
  
    public BitBucketOrganization(PageElement row)
    {
        this.row = row;
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
        
        PageElement ddButton = row.find(By.className("aui-dd-trigger"));
        ddButton.click();
        
        PageElement deleteLink = row.find(By.className("dvcs-control-delete-org"));
        deleteLink.click();

        //wait until marker is gone.
        Poller.waitUntil(elementFinder.find(By.id("Submit")).timed().hasClass("_posting"), is(false), by(30000));
    }

	public PageElement getRepositoriesTable()
	{
		return repositoriesTable;
	}
    
    
}
