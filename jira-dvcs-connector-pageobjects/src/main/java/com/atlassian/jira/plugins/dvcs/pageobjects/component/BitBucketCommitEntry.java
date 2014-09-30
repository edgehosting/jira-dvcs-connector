package com.atlassian.jira.plugins.dvcs.pageobjects.component;

import com.atlassian.pageobjects.elements.PageElement;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a commit entry that is displayed in the <tt>BitBucketIssuePanel</tt>
 */
public class BitBucketCommitEntry
{
    private final PageElement div;

    public BitBucketCommitEntry(PageElement div)
    {
        this.div = div;
    }

    /**
     * The message associated with this commit
     *
     * @return Message
     */
    public String getCommitMessage()
    {
        return div.findAll(By.className("CommitText")).get(0).getText();
    }

    public List<String> getCommitMessageLinks()
    {
        List<String> linkTexts = new ArrayList<String>();
        PageElement commitMessageDiv = div.find(By.className("CommitText"));
        List<PageElement> links = commitMessageDiv.findAll(By.tagName("a"));
        for (PageElement link : links)
        {
            linkTexts.add(link.getText());
        }
        return linkTexts;
    }


    public List<PageElement> getStatistics()
    {
        return div.findAll(By.className("CommitCount"));
    }

    public String getAdditions(PageElement pageElement)
    {
        return pageElement.find(By.className("CommitCountPlus")).getText();
    }

    public String getDeletions(PageElement pageElement)
    {
        return pageElement.find(By.className("CommitCountMinus")).getText();
    }

    public boolean isAdded(PageElement pageElement)
    {
        return pageElement.find(By.className("CommitCountAdded")).isPresent();
    }

    public boolean isDeleted(PageElement pageElement)
    {
        return pageElement.find(By.className("CommitCountRemoved")).isPresent();
    }
}
