package it.restart.com.atlassian.jira.plugins.dvcs;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.PageElement;

public class RepositoryDiv
{
    
    public RepositoryDiv(PageElement rootElement)
    {
        PageElement find = rootElement.find(By.tagName("td"));
    }

}
