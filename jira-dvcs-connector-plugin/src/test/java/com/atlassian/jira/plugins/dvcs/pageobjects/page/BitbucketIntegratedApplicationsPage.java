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

    @ElementBy(id = "add-consumer-link")
    private PageElement addConsumerButton;
  
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
        addConsumerButton.click();

        PageElement addOAuthConsumerDialogDiv = null;
        while (addOAuthConsumerDialogDiv == null)
        {
            addOAuthConsumerDialogDiv = PageElementUtils.findVisibleElementByClassName(bodyElement, "ui-dialog");
        }     

        BitbucketAddOAuthConsumerDialog addConsumerDialog =
                pageBinder.bind(BitbucketAddOAuthConsumerDialog.class, addOAuthConsumerDialogDiv);

        lastAddedConsumerName = "Test OAuth [" + System.currentTimeMillis() + "]";
        String consumerDescription = "Test OAuth Description [" + System.currentTimeMillis() + "]";

        addConsumerDialog.addConsumer(lastAddedConsumerName, consumerDescription);

        Poller.waitUntilFalse(addOAuthConsumerDialogDiv.timed().isVisible());

        PageElement oauthConsumerOrderedList = elementFinder.find(By.className("list-widget"));

        for (PageElement oauthConsumerListItem : oauthConsumerOrderedList.findAll(By.tagName("li")))
        {
            // 1st <div> is description
            // 3rd <div> is key
            // 4th <div> is secret
            List<PageElement> divElements = oauthConsumerListItem.findAll(By.tagName("div"));  

            boolean isRecentlyAddedConsumer = divElements.get(0).find(By.tagName("dd")).getText().equals(consumerDescription);

            if (isRecentlyAddedConsumer)
            {
                String generatedOauthKey    = divElements.get(2).find(By.tagName("dd")).getText();
                String generatedOauthSecret = divElements.get(3).find(By.tagName("dd")).getText();

                return new OAuthCredentials(generatedOauthKey, generatedOauthSecret);
            }
        }  

        return null;//TODO remove oauth consumers created because of tests
    }

    public void removeLastAdddedConsumer()
    {
        PageElement oauthConsumerOrderedList = elementFinder.find(By.className("list-widget"));

        for (PageElement oauthConsumerListItem : oauthConsumerOrderedList.findAll(By.tagName("li")))
        {
            PageElement expandConsumerLink = PageElementUtils.findTagWithAttribute(oauthConsumerListItem, "a", "class", "name");

            if (lastAddedConsumerName.equals(expandConsumerLink.getText()))
            {
                PageElement deleteConsumerButton = PageElementUtils.findTagWithText(oauthConsumerListItem, "a", "Delete");
                deleteConsumerButton.click();
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