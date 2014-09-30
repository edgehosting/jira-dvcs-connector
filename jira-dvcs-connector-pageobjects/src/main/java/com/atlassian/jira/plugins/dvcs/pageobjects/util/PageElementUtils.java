package com.atlassian.jira.plugins.dvcs.pageobjects.util;

import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public final class PageElementUtils
{
    private PageElementUtils() {}


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

    public static PageElement findTagWithAttributeValue(PageElement sourceElement, String tagName, String attributeName,
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

    public static PageElement findTagWithAttributeValueEndingWith(PageElement sourceElement, String tagName,
            String attributeName, String attributeValueEndPart)
    {
        for (PageElement tag : sourceElement.findAll(By.tagName(tagName)))
        {
            String attributeValue = tag.getAttribute(attributeName);
            if (attributeValue != null && attributeValue.endsWith(attributeValueEndPart))
            {
                return tag;
            }
        }

        return null;
    }

    /**
     * This is workaround for Permission denied to access property 'nr@context' issue.
     * Accessing page element e.g. reading it's tag name, before any action on the page.
     *
     * This must be removed after the issue is fixed in Selenium
     *
     * @param pageElement any page element to be used
     */
    public static void permissionDeniedWorkAround(PageElement pageElement)
    {
        try
        {
            pageElement.getTagName();
        }
        catch (Throwable t)
        {
            // ignoring any errors
        }
    }
}
