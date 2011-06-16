package com.atlassian.jira.plugins.bitbucket.property;

import java.io.Serializable;

/**
 * Describes the current progress through a repository sync. This object will indicate either:
 * <ol>
 *  <li>The sync has not yet started</li>
 *  <li>The sync is progress at a particular revision</li>
 *  <li>The sync has completed</li>
 * </ol>
 *
 */
public interface BitbucketSyncProgress extends Serializable
{
    class Completed implements BitbucketSyncProgress
    {
    }

    class InProgress implements BitbucketSyncProgress
    {
        public InProgress(int revision)
        {
        }
    }

    class NotStarted implements BitbucketSyncProgress
    {
    }
}
