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

    public String getMessage()
    {
        return rootElement.find(By.xpath("td[3]/div")).getText();
    }

    public String getRepositoryName()
    {
        return rootElement.find(By.xpath("td[2]/a")).getText();
    }

}