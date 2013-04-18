package com.atlassian.jira.plugins.dvcs.pageobjects.component;

import javax.inject.Inject;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

public class BitBucketOrganization
{
    private final PageElement row;

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
        PageElement ddButton = row.find(By.className("aui-dd-trigger"));
        ddButton.click();
        
        PageElement deleteLink = row.find(By.className("dvcs-control-delete-org"));
        deleteLink.click();

        ConfirmationDialog dialog = elementFinder.find(By.id("confirm-dialog"), ConfirmationDialog.class, TimeoutType.DIALOG_LOAD);
        dialog.confirm();
        dialog.waitUntilVisible();
    }

    public PageElement getRepositoriesTable()
    {
        return repositoriesTable;
    }
}
