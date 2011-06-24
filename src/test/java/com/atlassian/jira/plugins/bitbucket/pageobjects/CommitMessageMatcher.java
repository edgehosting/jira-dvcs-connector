package com.atlassian.jira.plugins.bitbucket.pageobjects;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketCommitEntry;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Checks whether the commit message matches the expected message
 */
public class CommitMessageMatcher extends TypeSafeMatcher<BitBucketCommitEntry>
{
    private final String expectedCommitMessage;

    public CommitMessageMatcher(String expected)
    {
        this.expectedCommitMessage = expected;
    }

    @Override
    public boolean matchesSafely(BitBucketCommitEntry bitBucketCommitEntry)
    {
        return bitBucketCommitEntry.getCommitMessage().equals(expectedCommitMessage);
    }

    public void describeTo(Description description)
    {
        description.appendText("commit message was not as expected.");
    }

    @Factory
    public static Matcher<BitBucketCommitEntry> withMessage(String expected)
    {
        return new CommitMessageMatcher(expected);
    }
}
