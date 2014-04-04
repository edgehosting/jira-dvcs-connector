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
    private static final String DYNAMIC_REPOSITORIES_PREFIX = "it.restart";

    @Inject
    private PageBinder pageBinder;

    @Inject
    private PageElementFinder elementFinder;
    
    private final PageElement rootElement;
    private final PageElement repositoriesTable;
    private final PageElement organizationType;
    private final PageElement organizationName;
  
    public OrganizationDiv(PageElement row)
    {
        this.rootElement = row;
        this.repositoriesTable = rootElement.find(By.tagName("table"));
        this.organizationType =  rootElement.find(By.xpath("div/h4"));
        this.organizationName = rootElement.find(By.xpath("div/h4/a"));
    }

    /**
     * Deletes this repository from the list
     */
    public void delete()
    {
        // add marker to wait for post complete
        PageElement ddButton = rootElement.find(By.className("aui-dropdown2-trigger"));
        ddButton.click();
        String dropDownMenuId = ddButton.getAttribute("aria-owns");
        PageElement deleteLink = elementFinder.find(By.id(dropDownMenuId)).find(By.className("dvcs-control-delete-org"));
        deleteLink.click();
        
        ConfirmationDialog dialog = elementFinder.find(By.id("confirm-dialog"), ConfirmationDialog.class, TimeoutType.DIALOG_LOAD);
        dialog.confirm();
        dialog.waitUntilVisible();
    }

    public List<RepositoryDiv> getRepositories()
    {
        return getRepositories(false);
    }

    public List<RepositoryDiv> getRepositories(boolean filterDynamicRepositories)
    {
        List<RepositoryDiv> list = new ArrayList<RepositoryDiv>();
        List<PageElement> trs = repositoriesTable.findAll(By.xpath("//table/tbody/tr"));
        for (PageElement tr : trs)
        {

            RepositoryDiv repositoryDiv = pageBinder.bind(RepositoryDiv.class, tr);
            if (!filterDynamicRepositories || !repositoryDiv.getRepositoryName().startsWith(DYNAMIC_REPOSITORIES_PREFIX))
            {
                list.add(pageBinder.bind(RepositoryDiv.class, tr));
            }
        }
        return list;
    }

    public boolean containsRepository(String name)
    {
        List<RepositoryDiv> repositories = getRepositories();
        for (RepositoryDiv repositoryDiv : repositories)
        {
            if (name.equals(repositoryDiv.getRepositoryName()))
            {
                return true;
            }
        }

        return false;
    }
    
    public String getOrganizationType()
    {
        // <h4 class="aui bitbucketLogo">
        return organizationType.getAttribute("class").replaceAll("aui (.*)Logo", "$1");
    }
    
    public String getOrganizationName()
    {
        return organizationName.getText();
    }
    
}
