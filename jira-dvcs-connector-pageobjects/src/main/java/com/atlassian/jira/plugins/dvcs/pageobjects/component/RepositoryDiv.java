package com.atlassian.jira.plugins.dvcs.pageobjects.component;

import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

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

    public PageElement getSyncIcon()
    {
        return rootElement.find(By.xpath("td[4]//span"));
    }

    public String getElementId()
    {
        return rootElement.getAttribute("id");
    }

    public String getRepositoryId()
    {
        return parseRepositoryId(getElementId());
    }

    public String parseRepositoryId(String elementId)
    {
        return elementId.substring(elementId.lastIndexOf("-") + 1);
    }
}
