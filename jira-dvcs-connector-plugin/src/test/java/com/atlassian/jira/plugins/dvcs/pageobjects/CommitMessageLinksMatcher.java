package com.atlassian.jira.plugins.dvcs.pageobjects;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;

/**
 * Checks whether the commit message matches the expected message
 */
public class CommitMessageLinksMatcher extends TypeSafeMatcher<BitBucketCommitEntry>
{
    private final List<String> expectedCommitMessageLinkTexts;

    public CommitMessageLinksMatcher(List<String> expectedLinkTexts)
    {
        expectedCommitMessageLinkTexts = expectedLinkTexts;
    }

    @Override
    public boolean matchesSafely(BitBucketCommitEntry bitBucketCommitEntry)
    {
        return CollectionUtils.isEqualCollection(bitBucketCommitEntry.getCommitMessageLinks(), expectedCommitMessageLinkTexts);
    }

    @Override
	public void describeTo(Description description)
    {
        description.appendText("commit message was not as expected.");
    }

    @Factory
    public static Matcher<BitBucketCommitEntry> withMessageLinks(String... expectedLinkTexts)
    {
        return new CommitMessageLinksMatcher(Arrays.asList(expectedLinkTexts));
    }
}
