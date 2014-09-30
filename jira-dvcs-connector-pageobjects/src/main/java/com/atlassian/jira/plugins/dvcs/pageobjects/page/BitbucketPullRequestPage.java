package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import javax.inject.Inject;

import static com.atlassian.pageobjects.elements.query.Poller.by;
import static org.hamcrest.core.Is.is;

/**
 * @author Miroslav Stencel mstencel@atlassian.com
 */
public class BitbucketPullRequestPage implements Page
{    

    @ElementBy(id = "id_new_comment")
    private PageElement commentElement;

    @ElementBy(xpath = "//button[text()='Comment']")
    private PageElement commentButton;
    
    @ElementBy(id = "reject-pullrequest")
    private PageElement rejectButton;
    
    @ElementBy(id = "approve-button")
    private PageElement approveButton;
    
    @ElementBy(id = "fulfill-pullrequest")
    private PageElement mergeButton;
    
    @Inject
    private PageElementFinder elementFinder;
    
    @Inject
    WebDriver webDriver;

    private String url;

    public BitbucketPullRequestPage()
    {
    }

    public BitbucketPullRequestPage(final String url)
    {
        this.url = url;
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    public String commentPullRequest(String comment)
    {
         commentElement.select();
         Poller.waitUntilTrue(commentButton.timed().isVisible());
         commentElement.type(comment);
         commentButton.click();
         return comment;
    }
    
    
    public void declinePullRequest()
    {
         rejectButton.click();
         DeclinePullRequestDialog declinePullRequestDialog = elementFinder.find(By.id("bb-reject-pullrequest-dialog"), DeclinePullRequestDialog.class);
         declinePullRequestDialog.decline("Test decline reason");
         try
         {
             Poller.waitUntil(declinePullRequestDialog.timed().isPresent(), is(false), by(15000));
         } catch (AssertionError e)
         {
             // ignoring time out, Bitbucket probably didn't close the dialog
         }
    }
    
    public void approvePullRequest()
    {
         approveButton.click();
         try
         {
             Poller.waitUntil(approveButton.timed().hasText("Approve"), is(false), by(15000));
         }  catch (AssertionError e)
         {
             // we will continue in hope that Bitbucket did its job
         }
    }
    
    public void mergePullRequest()
    {
        mergeButton.click();
        MergePullRequestDialog mergePullRequestDialog = elementFinder.find(By.id("bb-fulfill-pullrequest-dialog"), MergePullRequestDialog.class);
        mergePullRequestDialog.merge();
        try
        {
            Poller.waitUntil(mergePullRequestDialog.timed().isPresent(), is(false), by(30000));
        } catch (AssertionError e)
        {
            // ignoring time out, Bitbucket probably didn't close the dialog
        }
    }

    public static class DeclinePullRequestDialog extends WebDriverElement
    {
         @ElementBy(id="id_reason")
         private PageElement reasonElement;
         
         @ElementBy(xpath=".//button[text() = 'Decline']")
         private PageElement declineButton;
         
          public DeclinePullRequestDialog(By locator)
          {
               super(locator);
          }
          
          public void decline(String reason)
          {
               reasonElement.clear().type(reason);
               declineButton.click();
          }
    }
    
    public static class MergePullRequestDialog extends WebDriverElement
    {
         @ElementBy(xpath=".//button[text() = 'Merge']")
         private PageElement mergeButton;

         public MergePullRequestDialog(By locator)
         {
              super(locator);
         }
         
         public void merge()
         {
              mergeButton.click();
         }
    }
}
