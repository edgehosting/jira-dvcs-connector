package com.atlassian.jira.plugins.bitbucket.mapper;


import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.util.concurrent.BlockingReference;

import java.util.concurrent.Future;

public class Progress
{
    public interface State
    {
    }

    class Starting implements State
    {
    }

    public static class InProgress implements State
    {
        final int revision;

        public InProgress(int revision)
        {
            this.revision = revision;
        }

        public int getRevision()
        {
            return revision;
        }

        public String toString()
        {
            return String.valueOf(revision);
        }
    }

    private final SynchronizationKey key;
    private final Future<OperationResult> future;
    private final BlockingReference<State> progress = BlockingReference.newMRSW();

    public Progress(SynchronizationKey key, Future<OperationResult> future)
    {
        this.key = key;
        this.future = future;
        this.progress.set(new Starting());
    }

    public void setProgress(State progress)
    {
        this.progress.set(progress);
    }

    public State getProgress()
    {
        return progress.peek();
    }

    public SynchronizationKey getKey()
    {
        return key;
    }

    public boolean matches(String projectKey, RepositoryUri repositoryUri)
    {
        return key.matches(projectKey, repositoryUri);
    }

}