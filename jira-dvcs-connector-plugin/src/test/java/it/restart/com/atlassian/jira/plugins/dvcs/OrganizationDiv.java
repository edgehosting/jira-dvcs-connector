package it.restart.com.atlassian.jira.plugins.dvcs;

import static com.atlassian.pageobjects.elements.query.Poller.by;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Poller;

public class OrganizationDiv
{
    @Inject
    private PageBinder pageBinder;

    @Inject
    private WebDriver javaScriptExecutor;

    @Inject
    PageElementFinder elementFinder;
    
    private final PageElement rootElement;
    private final PageElement repositoriesTable;
    private final PageElement repositoryType;
    private final PageElement repositoryName;

    public OrganizationDiv(PageElement row)
    {
        this.rootElement = row;
        this.repositoriesTable = rootElement.find(By.tagName("table"));
        this.repositoryType = rootElement.find(By.xpath("div/h4"));
        this.repositoryName = rootElement.find(By.xpath("div/h4/a"));
    }

    /**
     * Deletes this repository from the list
     */
    public void delete()
    {
        // disable confirm popup
        ((JavascriptExecutor) javaScriptExecutor).executeScript("window.confirm = function(){ return true; }");
        // add marker to wait for post complete
        PageElement ddButton = rootElement.find(By.className("aui-dd-trigger"));
        ddButton.click();
        PageElement deleteLink = rootElement.find(By.className("dvcs-control-delete-org"));
        deleteLink.click();
        // wait for popup to show up
        try
        {
            Poller.waitUntilTrue(elementFinder.find(By.id("deleting-account-dialog")).timed().isVisible());
        } catch (AssertionError e)
        {
            // ignore, the deletion was probably very quick and the popup has been already closed.
        }
        Poller.waitUntil(elementFinder.find(By.id("deleting-account-dialog")).timed().isVisible(), is(false), by(30000));
    }

    public List<OrganizationRepositoryRow> getRepositories()
    {
        if (repositoriesTable.isPresent()) {
            return repositoriesTable.findAll(By.xpath("//table/tbody/tr"), OrganizationRepositoryRow.class);
        } else {
            Assert.assertTrue(rootElement.find(By.xpath(".//span[contains(concat(' ', @class, ' '), ' dvcs-no-repos ')]")).isPresent());
            return Collections.emptyList();
        }
    }

    /**
     * @param repositoryName
     *            name of searched repository
     * @return founded {@link OrganizationRepositoryRow}
     */
    public OrganizationRepositoryRow getRepository(String repositoryName)
    {
        return repositoriesTable.find(
                By.xpath("//table/tbody/tr/td[@class='dvcs-org-reponame']/a[text()='" + repositoryName + "']/ancestor::tr"),
                OrganizationRepositoryRow.class);
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
