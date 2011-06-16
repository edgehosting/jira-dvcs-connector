package com.atlassian.jira.plugins.bitbucket.property;

/**
 * Describes the current progress through a repository sync. This object will indicate either:
 * <ol>
 *  <li>The sync is progress at a particular revision or is at the tip</li>
 *  <li>The sync has completed</li>
 * </ol>
 *
 */
public class BitbucketSyncProgress
{
    public static BitbucketSyncProgress completed()
    {
        return new BitbucketSyncProgress(true);
    }

    public static BitbucketSyncProgress progress(int revision)
    {
        return new BitbucketSyncProgress(revision);
    }

    public static BitbucketSyncProgress tip()
    {
        return new BitbucketSyncProgress();
    }

    private final boolean completed;
    private final boolean tip;
    private final int revision;

    private BitbucketSyncProgress()
    {
        this.completed = false;
        this.tip = true;
        this.revision = -1;
    }

    private BitbucketSyncProgress(boolean completed)
    {
        this.completed = completed;
        this.tip = false;
        this.revision = 0;
    }

    private BitbucketSyncProgress(int revision)
    {
        this.tip = false;
        this.completed = false;
        this.revision = revision;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isTip()
    {
        return tip;
    }

    public int getRevision()
    {
        return revision;
    }
}
