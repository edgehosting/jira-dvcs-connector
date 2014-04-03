package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import org.openqa.selenium.By;

import com.atlassian.jira.plugins.dvcs.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.WebDriverException;

/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public class BitbucketGrandOAuthAccessPage implements Page
{    
    @ElementBy(tagName= "body")
    private PageElement bodyElement;

 
    
    @Override
    public String getUrl()
    {
        return "not really needed";
    }
    
    
    public void grantAccess()
    {
        // <div class="submit">
        //     <input type="submit" value="Grant access">
        // </div>
        
        PageElement buttonsDiv = bodyElement.find(By.className("buttons"));

        try
        {
            bodyElement.getText();
        }
        catch (WebDriverException e)
        {
            // workaround for Permission denied to access property 'nr@context' issue
        }

        PageElement grandAccessButton = PageElementUtils.findTagWithAttributeValue(buttonsDiv,
                                                                                   "button",
                                                                                   "type",
                                                                                   "submit");
        
        grandAccessButton.click();
    }
}
