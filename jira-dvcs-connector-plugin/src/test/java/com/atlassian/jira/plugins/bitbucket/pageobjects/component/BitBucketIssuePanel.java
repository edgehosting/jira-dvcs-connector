package com.atlassian.jira.plugins.bitbucket.pageobjects.component;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.components.ActivatedComponent;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.openqa.selenium.By;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the BitBucket panel in the view issue page
 */
public class BitBucketIssuePanel implements ActivatedComponent<BitBucketIssuePanel>
{
    @Inject
    PageBinder pageBinder;

    @ElementBy(id="bitbucket-commits-tabpanel")
    PageElement trigger;

    @ElementBy(id="issue_actions_container")
    PageElement view;

    public PageElement getTrigger()
    {
        return trigger;
    }

    public PageElement getView()
    {
        return view;
    }

    public BitBucketIssuePanel open()
    {
         if(!isOpen())
        {
            trigger.click();
            Poller.waitUntilTrue(trigger.timed().hasClass("active"));
        }
        return this;
    }

    public boolean isOpen()
    {
        return trigger.hasClass("active");
    }

    /**
     * Waits for commits to be retrieved from GitHub
     * @return List of <tt>BitBucketCommitEntry</tt>
     */
    public List<BitBucketCommitEntry> waitForMessages()
    {
        // wait for one message to be present (setting timeout type to longest value)
        Poller.waitUntilTrue(view.find(By.className("CommitContainer"), TimeoutType.PAGE_LOAD).timed().isPresent());

        //get all the messages
        List<BitBucketCommitEntry> commitMessageList = new ArrayList<BitBucketCommitEntry>();
        for(PageElement div : view.findAll(By.className("CommitContainer")))
        {
            commitMessageList.add(pageBinder.bind(BitBucketCommitEntry.class, div));
        }

        return commitMessageList;
    }
}
