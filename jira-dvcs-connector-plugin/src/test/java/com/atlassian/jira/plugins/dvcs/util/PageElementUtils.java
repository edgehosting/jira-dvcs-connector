package com.atlassian.jira.plugins.dvcs.util;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.PageElement;

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
}
