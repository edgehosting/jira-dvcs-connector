package com.atlassian.jira.plugins.dvcs.util;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.AtlassianWebDriver;
import com.google.common.base.Function;

import org.openqa.selenium.WebDriver;

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

    public static void waitUntilPageUrlDoesNotContain(AtlassianWebDriver webDriver, final String urlPart)//TODO remove
    {
        webDriver.waitUntil(new WebdriverPageUrlContainsPredicate(urlPart, false));
    }

    private static final class WebdriverPageUrlContainsPredicate implements Function<WebDriver, Boolean>
    {
        private final String urlPart;
        private final boolean contains; // contains or does not contain

        private WebdriverPageUrlContainsPredicate(String urlPart, boolean contains)
        {
            this.urlPart  = urlPart;
            this.contains = contains;
        }

        @Override
        public Boolean apply(WebDriver webDriver)
        {
            return contains ?  webDriver.getCurrentUrl().contains(urlPart)
                            : !webDriver.getCurrentUrl().contains(urlPart);
        }
    }
}
