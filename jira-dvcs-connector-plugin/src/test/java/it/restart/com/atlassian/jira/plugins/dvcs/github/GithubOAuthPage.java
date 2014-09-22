package it.restart.com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;
import javax.inject.Inject;

/**
 *
 */
public class GithubOAuthPage implements Page
{
    @ElementBy(name = "oauth_application[name]")
    private PageElement oauthApplicationName;

    @ElementBy(name = "oauth_application[url]")
    private PageElement oauthApplicationUrl;

    @ElementBy(name = "oauth_application[callback_url]")
    private PageElement oauthApplicationCallbackUrl;

    @ElementBy(cssSelector = ".new_oauth_application button")
    private PageElement submitButton;
    
    @ElementBy(cssSelector = ".keys")
    private PageElement secrets;
    
    @ElementBy(linkText = "Delete application")
    private PageElement deleteApplication;

    @ElementBy(xpath = "//div[@id='facebox']//button")
    private PageElement deleteApplicationConfirm;

    @ElementBy(tagName = "body")
    private PageElement body;

    @Inject
    private WebDriver webDriver;

    private final String hostUrl;

    public GithubOAuthPage()
    {
        this("https://github.com");
    }
    
    public GithubOAuthPage(String hostUrl)
    {
        this.hostUrl = hostUrl;
    }

    @Override
    public String getUrl()
    {
        return hostUrl + "/settings/applications/new";
    }

    public OAuth addConsumer(String jiraBaseUrl)
    {
        // register app
        String consumerName = "Test_OAuth_" + System.currentTimeMillis();
        oauthApplicationName.type(consumerName);
        oauthApplicationUrl.type(jiraBaseUrl);
        oauthApplicationCallbackUrl.type(jiraBaseUrl);
        submitButton.click();
        Poller.waitUntilTrue(secrets.timed().isVisible());
        
        // read credentials
        List<PageElement> allSecretsElements = secrets.findAll(By.tagName("dd"));
        String clientId = allSecretsElements.get(0).getText();
        String clientSecret = allSecretsElements.get(1).getText();
        String oauthAppUrl = body.find(By.xpath("//div[@class='boxed-group-inner']//form")).getAttribute("action");
        return new OAuth(clientId, clientSecret, oauthAppUrl);
    }

    public void removeConsumer()
    {
        deleteApplication.click();

        try
        {
            Poller.waitUntilTrue(deleteApplicationConfirm.timed().isVisible());
        }
        catch(AssertionError e)
        {
            // retryning the delete after page refresh, sometimes it's not working
            retry(5, new Runnable()
            {
                @Override
                public void run()
                {
                    webDriver.navigate().refresh();
                    deleteApplication.click();
                    Poller.waitUntilTrue(deleteApplicationConfirm.timed().isVisible());
                }
            });
        }
        deleteApplicationConfirm.click();
    }

    private void retry(int times, Runnable runnable)
    {
        for (int count = 1 ; count < times; count++)
        {
            try
            {
                runnable.run();
                return;
            }
            catch (AssertionError e)
            {
                // ignoring
            }
        }

        runnable.run();
    }

}
