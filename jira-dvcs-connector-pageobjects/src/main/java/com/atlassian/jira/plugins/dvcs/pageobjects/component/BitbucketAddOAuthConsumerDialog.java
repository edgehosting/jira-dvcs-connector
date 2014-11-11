package com.atlassian.jira.plugins.dvcs.pageobjects.component;

import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public class BitbucketAddOAuthConsumerDialog
{      
    private final PageElement addOAuthConsumerDialog;
    
    
    public BitbucketAddOAuthConsumerDialog(PageElement addOAuthConsumerDialog)
    {
        this.addOAuthConsumerDialog = addOAuthConsumerDialog;
    }
    
    
    public void addConsumer(String consumerName, String consumerDescription)
    {       
        PageElement consumerDescriptionInput = addOAuthConsumerDialog.find(By.id("consumer-description"));
        
        // for some unknown reason this must be set before consumer name, otherwise consumer name will not be set at all
        consumerDescriptionInput.type(consumerDescription);
        
        PageElement consumerNameInput = addOAuthConsumerDialog.find(By.id("consumer-name"));        
        consumerNameInput.click().type(consumerName);
       
        PageElement addConsumerButton = addOAuthConsumerDialog.find(By.className("button-panel-button"));

        addConsumerButton.click();
    }
}
