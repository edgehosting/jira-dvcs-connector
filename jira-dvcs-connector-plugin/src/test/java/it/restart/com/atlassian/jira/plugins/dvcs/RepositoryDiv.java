package it.restart.com.atlassian.jira.plugins.dvcs;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.PageElement;

public class RepositoryDiv
{
    private final PageElement rootElement;

    public RepositoryDiv(PageElement rootElement)
    {
        this.rootElement = rootElement;
    }

    public boolean isSyncing()
    {
        PageElement syncRepoIcon = rootElement.find(By.xpath("td[4]/div/a/span"));
        if (syncRepoIcon!=null)
        {
            return syncRepoIcon.getAttribute("class").contains("running");
        }
        return false;
    }
    
    public String getMessage()
    {
        return rootElement.find(By.xpath("td[3]/div")).getText();
    }

    public String getRepositoryName()
    {
        return rootElement.find(By.xpath("td[2]/a")).getText();
    }

}
