package com.atlassian.jira.plugins.dvcs.pageobjects.bitbucket;

import com.atlassian.jira.plugins.dvcs.pageobjects.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

public class BitbucketGrantAccessPage implements Page
{    
    @ElementBy(tagName= "body")
    private PageElement bodyElement;
    
    @Override
    public String getUrl()
    {
        throw new UnsupportedOperationException();
    }
    
    public void grantAccess()
    {
        PageElement buttonsDiv = bodyElement.find(By.className("buttons"));

        // accessing tag name as workaround for permission denied to access property 'nr@context' issue
        PageElementUtils.permissionDeniedWorkAround(bodyElement);

        PageElement grantAccessButton = PageElementUtils.findTagWithAttributeValue(buttonsDiv, "button", "type", "submit");
        grantAccessButton.click();
    }
}
