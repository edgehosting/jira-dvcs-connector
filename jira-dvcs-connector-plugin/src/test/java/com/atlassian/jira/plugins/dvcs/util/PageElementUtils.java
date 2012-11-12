package com.atlassian.jira.plugins.dvcs.util;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.AtlassianWebDriver;

/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public final class PageElementUtils
{
    private PageElementUtils() {}


    public static PageElement findVisibleElementByClassName(PageElement sourceElement, String className)//TODO remove
    {
        for (PageElement pageElement : sourceElement.findAll(By.className(className)))
        {
            String styleAttributeValue = pageElement.getAttribute("style");

            if (!styleAttributeValue.contains("display: none;"))
            {
                return pageElement;
            }
        }

        return null;
    }

    public static PageElement findTagWithAttribute(PageElement sourceElement, String tagName, String attributeName)
    {
        for (PageElement tag : sourceElement.findAll(By.tagName(tagName)))
        {
            if (tag.getAttribute(attributeName) != null)
            {
                return tag;
            }
        }

        return null;
    }

    public static PageElement findTagWithAttribute(PageElement sourceElement, String tagName, String attributeName,
            String attributeValue)
    {
        for (PageElement tag : sourceElement.findAll(By.tagName(tagName)))
        {
            if (attributeValue.equals(tag.getAttribute(attributeName)))
            {
                return tag;
            }
        }

        return null;
    }

    public static PageElement findTagWithText(PageElement sourceElement, String tagName, String expectedTagText)
    {
        for (PageElement tag : sourceElement.findAll(By.tagName(tagName)))
        {
            if (expectedTagText.equals(tag.getText()))
            {
                return tag;
            }
        }

        return null;
    }

    public static void waitUntilPageUrlDoesNotContain(AtlassianWebDriver webDriver, String string)
    {
        String pageUrl = webDriver.getCurrentUrl();

        while (pageUrl.contains(string))
        {
            try
            {
                Thread.sleep(100);
                pageUrl = webDriver.getCurrentUrl();
            }
            catch (InterruptedException e) {} // does not matter
        }
    }

    public static void waitUntilPageUrlContains(AtlassianWebDriver webDriver, String string)
    {
        String pageUrl = webDriver.getCurrentUrl();

        while (!pageUrl.contains(string))
        {
            try
            {
                Thread.sleep(100);
                pageUrl = webDriver.getCurrentUrl();
            }
            catch (InterruptedException e) {} // does not matter
        }
    }
}
