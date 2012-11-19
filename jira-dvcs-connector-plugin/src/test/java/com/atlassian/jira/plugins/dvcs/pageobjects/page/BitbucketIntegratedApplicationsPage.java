package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import java.util.List;

import javax.inject.Inject;

import org.openqa.selenium.By;

import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitbucketAddOAuthConsumerDialog;
import com.atlassian.jira.plugins.dvcs.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Poller;

/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public class BitbucketIntegratedApplicationsPage implements Page
{    
    public static final String PAGE_URL = "https://bitbucket.org/account/user/jirabitbucketconnector/api";


    @Inject
    PageBinder pageBinder;

    @ElementBy(id = "oauth-consumers")
    private PageElement consumersConfg;
  
    @ElementBy(tagName= "body")
    private PageElement bodyElement;

    @Inject
    PageElementFinder elementFinder;
    
    private String lastAddedConsumerName;


    @Override
    public String getUrl()
    {
        return PAGE_URL;
    }


    public OAuthCredentials addConsumer()
    {       
        
        addConsumerButton().click();

        PageElement addOAuthConsumerDialogDiv = null;
        while (addOAuthConsumerDialogDiv == null)
        {
            addOAuthConsumerDialogDiv = bodyElement.find(By.id("bb-add-consumer-dialog"));
        }     

        BitbucketAddOAuthConsumerDialog addConsumerDialog =
                pageBinder.bind(BitbucketAddOAuthConsumerDialog.class, addOAuthConsumerDialogDiv);

        lastAddedConsumerName = "Test OAuth [" + System.currentTimeMillis() + "]";
        String consumerDescription = "Test OAuth Description [" + System.currentTimeMillis() + "]";

        addConsumerDialog.addConsumer(lastAddedConsumerName, consumerDescription);

        Poller.waitUntilFalse(addOAuthConsumerDialogDiv.timed().isVisible());

        List<PageElement> allConsumers = elementFinder.findAll(By.className("extra-info"));
        PageElement consumer = allConsumers.get(allConsumers.size() - 1);
        String key = getKey(consumer);
        String secret = getSecret(consumer);

        return new OAuthCredentials(key, secret);
 
    }

    private String getSecret(PageElement consumer)
    {
        return consumer.findAll(By.tagName("span")).get(3).getText();
    }


    private String getKey(PageElement consumer)
    {
        return consumer.findAll(By.tagName("span")).get(2).getText();
    }


    private PageElement addConsumerButton()
    {
        List<PageElement> all = consumersConfg.findAll(By.tagName("a"));
        for (PageElement pageElement : all)
        {
            if ("#add-consumer".equals(pageElement.getAttribute("href"))) {
                return pageElement;
            }
        }
        
        throw new IllegalStateException("Add consumer button not found.");
    }


    public void removeLastAdddedConsumer()
    {
        PageElement oauthConsumersSection = elementFinder.find(By.id("oauth-consumers"));
        
        for (PageElement oauthConsumerRow : oauthConsumersSection.findAll(By.tagName("tr")))
        {
            PageElement oauthConsumerNameSpan = oauthConsumerRow.find(By.tagName("span"));
            
            if (oauthConsumerNameSpan.isPresent() && // first row is table head not containing span
                oauthConsumerNameSpan.getText().equals(lastAddedConsumerName))
            {
                PageElement deleteConsumerButton = PageElementUtils.findTagWithAttribute(oauthConsumerRow, "a", "href", "#delete");
                deleteConsumerButton.click();
                break;
            }
        }
    }    

    public static final class OAuthCredentials
    {
        public final String oauthKey;
        public final String oauthSecret; 

        private OAuthCredentials(String oauthKey, String oauthSecret)
        {
            this.oauthKey = oauthKey;
            this.oauthSecret = oauthSecret;
        }
    }
}