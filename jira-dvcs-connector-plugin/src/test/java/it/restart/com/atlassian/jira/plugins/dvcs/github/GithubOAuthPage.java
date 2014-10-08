package it.restart.com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.inject.Inject;

import static com.atlassian.pageobjects.elements.timeout.TimeoutType.PAGE_LOAD;

/**
 *
 */
public class GithubOAuthPage implements Page
{
    private static Logger logger = LoggerFactory.getLogger(GithubOAuthPage.class);

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

    @ElementBy(linkText = "Delete application", timeoutType = PAGE_LOAD)
    private PageElement deleteApplication;

    @ElementBy(xpath = "//div[@id='facebox']//button", timeoutType = PAGE_LOAD)
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

        final Runnable remove = new Runnable()
        {
            @Override
            public void run()
            {
                deleteApplication.click();
                Poller.waitUntilTrue(deleteApplicationConfirm.timed().isVisible());
                deleteApplicationConfirm.click();
            }
        };
        try
        {
            remove.run();
        }
        catch(AssertionError e)
        {
            // retrying the delete after page refresh, sometimes it's not working
            retry(5, new Runnable()
            {
                @Override
                public void run()
                {
                    webDriver.navigate().refresh();
                    remove.run();
                }
            });
        }
    }

    private void retry(int times, Runnable runnable)
    {
        for (int count = 1 ; count <= times; count++)
        {
            logger.warn("retrying GitHub deleting application: {} times", count);
            try
            {
                runnable.run();
                return;
            }
            catch (AssertionError e)
            {
                // wait for a while and retry
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException ignored)
                {
                }
            }
        }
        logger.warn("Gave up retrying GitHub deleting application", new RuntimeException("for stack tracing only"));
    }

}
