package com.atlassian.jira.plugins.dvcs.util;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.PageElement;

/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public final class PageElementUtils
{
    private PageElementUtils() {}


    public static PageElement getVisibleElementByClassName(PageElement sourceElement, String className)
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
}
