package com.atlassian.jira.plugins.bitbucket.property;

import com.atlassian.jira.plugins.bitbucket.webwork.BitbucketCommits;

/**
 * Describes the current progress through a repository sync. This object will indicate either:
 * <ol>
 * <li>The sync processing a page starting at the <strong>tip</strong> or between the
 * {@link #getStartRevision() start} and {@link #getEndRevision() end} revisions.</li>
 * <li>The sync has completed</li>
 * <li>The progress of the sync is unknown</li>
 * </ol>
 */
public class BitbucketSyncProgress
{
    public static BitbucketSyncProgress completed()
    {
        return new BitbucketSyncProgress(false, true, false, 0);
    }

    public static BitbucketSyncProgress progress(int revision)
    {
        return new BitbucketSyncProgress(false, false, false, revision);
    }

    public static BitbucketSyncProgress tip()
    {
        return new BitbucketSyncProgress(false, false, true, 0);
    }

    public static BitbucketSyncProgress unknown()
    {
        return new BitbucketSyncProgress(true, false, false, 0);
    }

    private final boolean unknown;
    private final boolean completed;
    private final boolean tip;
    private final int startRevision;

    public BitbucketSyncProgress(boolean unknown, boolean completed, boolean tip, int startRevision)
    {
        this.unknown = unknown;
        this.completed = completed;
        this.tip = tip;
        this.startRevision = startRevision;
    }

    public boolean isCompleted()
    {
        return completed;
    }

    public boolean isTip()
    {
        return tip;
    }

    public boolean isUnknown()
    {
        return unknown;
    }

    public int getStartRevision()
    {
        return startRevision;
    }

    public int getEndRevision()
    {
        return Math.max(0, startRevision - BitbucketCommits.PAGE_SIZE);
    }

}
