package com.atlassian.jira.plugins.bitbucket.pageobjects.component;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.components.ActivatedComponent;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

/**
 * Represents the BitBucket panel in the view issue page
 */
public class BitBucketIssuePanel implements ActivatedComponent<BitBucketIssuePanel>
{
    @Inject
    PageBinder pageBinder;

    @ElementBy(id="dvcs-commits-tabpanel")
    PageElement trigger;

    @ElementBy(id="issue_actions_container")
    PageElement view;

    @Override
	public PageElement getTrigger()
    {
        return trigger;
    }

    @Override
	public PageElement getView()
    {
        return view;
    }

    @Override
	public BitBucketIssuePanel open()
    {
         if(!isOpen())
        {
            trigger.click();
            Poller.waitUntilTrue(trigger.timed().hasClass("active"));
        }
        return this;
    }

    @Override
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
        Poller.waitUntilTrue(view.find(By.className("message-container"), TimeoutType.PAGE_LOAD).timed().isPresent());

        //get all the messages
        List<BitBucketCommitEntry> commitMessageList = new ArrayList<BitBucketCommitEntry>();
        for(PageElement div : view.findAll(By.className("message-container")))
        {
            commitMessageList.add(pageBinder.bind(BitBucketCommitEntry.class, div));
        }

        return commitMessageList;
    }
}
