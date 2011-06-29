package com.atlassian.jira.plugins.bitbucket.property;

/**
 * Describes the current progress through a repository sync. This object will indicate either:
 * <ol>
 * <li>The sync is processing and has progressed to the {@link #getRevision()} revision.</li>
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
    private final int revision;

    public BitbucketSyncProgress(boolean unknown, boolean completed, boolean tip, int revision)
    {
        this.unknown = unknown;
        this.completed = completed;
        this.tip = tip;
        this.revision = revision;
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

    public int getRevision()
    {
        return revision;
    }

}
