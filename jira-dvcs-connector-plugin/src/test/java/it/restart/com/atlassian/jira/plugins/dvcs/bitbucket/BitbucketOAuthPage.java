package it.restart.com.atlassian.jira.plugins.dvcs.bitbucket;

import com.atlassian.jira.plugins.dvcs.util.PageElementUtils;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

public class BitbucketOAuthPage implements Page
{    
    @ElementBy(linkText = "Add consumer")
    private PageElement addConsumerButton;
    
    @ElementBy(id = "bb-add-consumer-dialog")
    private PageElement bbAddConsumerDialog;
    
    @ElementBy(id = "consumer-name")
    private PageElement consumerNameInput;
    
    @ElementBy(id = "consumer-description")
    private PageElement consumerDescriptionInput;
    
    @ElementBy(tagName = "body")
    private PageElement body;
    
    @ElementBy(xpath = "//section[@id='oauth-consumers']//tbody")
    private PageElement consumersTable;

    private String account = "jirabitbucketconnector";

    public BitbucketOAuthPage()
    {
    }

    public BitbucketOAuthPage(final String account)
    {
        this.account = account;
    }

    @Override
    public String getUrl()
    {
        return "https://bitbucket.org/account/user/" + account + "/api";
    }

    public OAuth addConsumer()
    {
        // accessing tag name as workaround for permission denied to access property 'nr@context' issue
        PageElementUtils.permissionDeniedWorkAround(addConsumerButton);

        addConsumerButton.click();
        Poller.waitUntilTrue(bbAddConsumerDialog.timed().isVisible());
        String consumerName = "Test_OAuth_" + System.currentTimeMillis();
        String consumerDescription = "Test OAuth Description [" + consumerName + "]";
        consumerNameInput.click().type(consumerName);
        consumerDescriptionInput.type(consumerDescription);
        bbAddConsumerDialog.find(By.className("button-panel-button")).click();
        Poller.waitUntilFalse(bbAddConsumerDialog.timed().isVisible());

        return parseOAuthCredentials();
    }

    private OAuth parseOAuthCredentials()
    {
        String applicationId = consumersTable.find(By.xpath("tr[@class='revealed']")).getAttribute("data-id");
        String key = consumersTable.find(By.xpath("tr[last()]//li[3]/span")).getText();
        String secret = consumersTable.find(By.xpath("tr[last()]//li[4]/span")).getText();
        
        return new OAuth(key, secret, applicationId);
    }

    public void removeConsumer(String applicationId)
    {
        PageElement oauthConsumer = body.find(By.id("consumer-" + applicationId));
        PageElement deleteButton = oauthConsumer.find(By.linkText("Delete"));

        // accessing tag name as workaround for permission denied to access property 'nr@context' issue
        PageElementUtils.permissionDeniedWorkAround(deleteButton);

        deleteButton.click();
    }
}