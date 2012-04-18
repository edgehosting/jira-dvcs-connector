package com.atlassian.jira.plugins.bitbucket.pageobjects;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketCommitEntry;
import org.apache.commons.collections.CollectionUtils;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Arrays;
import java.util.List;

/**
 * Checks whether the commit message matches the expected message
 */
public class CommitMessageLinksMatcher extends TypeSafeMatcher<BitBucketCommitEntry>
{
    private final List<String> expectedCommitMessageLinkTexts;

    public CommitMessageLinksMatcher(List<String> expectedLinkTexts)
    {
        this.expectedCommitMessageLinkTexts = expectedLinkTexts;
    }

    @Override
    public boolean matchesSafely(BitBucketCommitEntry bitBucketCommitEntry)
    {
        return CollectionUtils.isEqualCollection(bitBucketCommitEntry.getCommitMessageLinks(), expectedCommitMessageLinkTexts);
    }

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
