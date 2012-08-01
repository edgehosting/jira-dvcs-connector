package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import org.openqa.selenium.By;

import com.atlassian.jira.plugins.dvcs.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

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
    
    
    public void grandAccess()
    {
        // <div class="submit">
        //     <input type="submit" value="Grant access">
        // </div>
        
        PageElement grandAccessDiv = bodyElement.find(By.className("submit"));
        
        PageElement grandAccessButton = PageElementUtils.findTagWithAttribute(grandAccessDiv,
                                                                              "input",
                                                                              "value",
                                                                              "Grant access");
        
        grandAccessButton.click();
    }
}
