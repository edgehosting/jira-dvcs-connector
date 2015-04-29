package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuthUtils;
import com.atlassian.jira.plugins.dvcs.pageobjects.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.testing.rule.WebDriverSupport;
import org.openqa.selenium.By;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.timeout.TimeoutType.PAGE_LOAD;
import static org.fest.assertions.api.Assertions.assertThat;

public class BitbucketOAuthPage implements Page
{
    @ElementBy(linkText = "Add consumer")
    private PageElement addConsumerButton;
    
    @ElementBy(id = "bb-add-consumer-dialog", timeoutType = PAGE_LOAD)
    private PageElement bbAddConsumerDialog;
    
    @ElementBy(id = "consumer-name")
    private PageElement consumerNameInput;
    
    @ElementBy(id = "consumer-description")
    private PageElement consumerDescriptionInput;
    
    @ElementBy(tagName = "body")
    private PageElement body;
    
    @ElementBy(xpath = "//section[@id='oauth-consumers']//tbody")
    private PageElement consumersTable;

    private final String account;

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

        // clicking the button scrolls to it, scroll back to top so that the dialog is considered visible
        WebDriverSupport.fromAutoInstall().getDriver().executeScript("scroll(0, 0);");

        Poller.waitUntilTrue(bbAddConsumerDialog.timed().isVisible());
        String consumerName = OAuthUtils.generateTestOAuthName();
        String consumerDescription = "Test OAuth Description [" + consumerName + "]";
        consumerNameInput.click().type(consumerName);
        consumerDescriptionInput.type(consumerDescription);
        bbAddConsumerDialog.find(By.className("button-panel-button")).click();
        waitUntilFalse(bbAddConsumerDialog.timed().isVisible());

        return parseOAuthCredentials();
    }

    private OAuth parseOAuthCredentials()
    {
        // retrieve oauth details and fail early when we could not get them (maybe due to BB UI changes)
        String applicationId = consumersTable.find(By.xpath("tr[@class='revealed']")).getAttribute("data-id");
        assertThat(applicationId).overridingErrorMessage("newly added oauth consumer app id should not be empty").isNotEmpty();
        String key = consumersTable.find(By.xpath("tr[last()]//span[@class='oauth-key']")).getText();
        assertThat(key).overridingErrorMessage("newly added oauth key should not be empty").isNotEmpty();
        String secret = consumersTable.find(By.xpath("tr[last()]//span[@class='oauth-secret']")).getText();
        assertThat(secret).overridingErrorMessage("newly added oauth secret should not be empty").isNotEmpty();

        return new OAuth(key, secret, applicationId);
    }

    public void removeConsumer(String applicationId)
    {
        PageElement oauthConsumer = body.find(By.id("consumer-" + applicationId));

        // click to show the actions inline dialog
        oauthConsumer.find(By.className("actions")).find(By.tagName("button")).click();

        // click on the Delete button
        final PageElement inlineDialog = body.find(By.id("consumer-actions-" + applicationId));
        final PageElement deleteButton = inlineDialog.find(By.linkText("Delete"));
        deleteButton.click();
    }
}
