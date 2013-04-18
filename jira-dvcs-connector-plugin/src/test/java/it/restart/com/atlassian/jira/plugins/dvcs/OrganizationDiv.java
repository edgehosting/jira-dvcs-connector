package it.restart.com.atlassian.jira.plugins.dvcs;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.openqa.selenium.By;

import com.atlassian.jira.plugins.dvcs.pageobjects.component.ConfirmationDialog;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

public class OrganizationDiv
{
    @Inject
    private PageBinder pageBinder;
    
    @Inject
    private PageElementFinder elementFinder;
    
    private final PageElement rootElement;
    private final PageElement repositoriesTable;
    private final PageElement repositoryType;
    private final PageElement repositoryName;
  
    public OrganizationDiv(PageElement row)
    {
        this.rootElement = row;
        this.repositoriesTable = rootElement.find(By.tagName("table"));
        this.repositoryType =  rootElement.find(By.xpath("div/h4"));
        this.repositoryName = rootElement.find(By.xpath("div/h4/a"));
    }

    /**
     * Deletes this repository from the list
     */
    public void delete()
    {
        // add marker to wait for post complete
        PageElement ddButton = rootElement.find(By.className("aui-dd-trigger"));
        ddButton.click();
        PageElement deleteLink = rootElement.find(By.className("dvcs-control-delete-org"));
        deleteLink.click();
        
        ConfirmationDialog dialog = elementFinder.find(By.id("confirm-dialog"), ConfirmationDialog.class, TimeoutType.DIALOG_LOAD);
        dialog.confirm();
        dialog.waitUntilVisible();
    }

    public List<RepositoryDiv> getRepositories()
    {
        List<RepositoryDiv> list = new ArrayList<RepositoryDiv>();
        List<PageElement> trs = repositoriesTable.findAll(By.xpath("//table/tbody/tr"));
        for (PageElement tr : trs)
        {
            list.add(pageBinder.bind(RepositoryDiv.class, tr));
        }
        return list;
    }
    
    public String getRepositoryType()
    {
        // <h4 class="aui bitbucketLogo">
        return repositoryType.getAttribute("class").replaceAll("aui (.*)Logo", "$1");
    }
    
    public String getRepositoryName()
    {
        return repositoryName.getText();
    }
    
}
