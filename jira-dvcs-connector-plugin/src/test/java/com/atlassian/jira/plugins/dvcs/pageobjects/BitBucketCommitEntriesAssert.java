package com.atlassian.jira.plugins.dvcs.pageobjects;


import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import org.fest.assertions.api.ListAssert;

import java.util.List;


/**
 * @author Martin Skurla
 */
public class BitBucketCommitEntriesAssert extends ListAssert<BitBucketCommitEntry>
{
    private BitBucketCommitEntriesAssert(List<BitBucketCommitEntry> commitEntries)
    {
        super(commitEntries);
    }
  

    public static BitBucketCommitEntriesAssert assertThat(List<BitBucketCommitEntry> commitEntries)
    {
        return new BitBucketCommitEntriesAssert(commitEntries);
    }


    public BitBucketCommitEntriesAssert hasItemWithCommitMessage(String expectedMessage)
    {
        for (BitBucketCommitEntry bitBucketCommitEntry : actual)
        {
            if (bitBucketCommitEntry.getCommitMessage().equals(expectedMessage))
            {
                return this;
            }
        }
        
        throw new AssertionError("No BitBucketCommitEntry with message '" + expectedMessage + "'");
    }
}
