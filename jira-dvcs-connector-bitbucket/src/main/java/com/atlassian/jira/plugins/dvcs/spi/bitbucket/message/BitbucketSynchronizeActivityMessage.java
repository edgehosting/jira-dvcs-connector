package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;

public class BitbucketSynchronizeActivityMessage implements Serializable, HasProgress
{

    private static final long serialVersionUID = -4361088769277502144L;

    private boolean softSync;

    private Repository repository;
    private Progress progress;
    private Set<Integer> processedPullRequests;
    private Set<Integer> processedPullRequestsLocal;

    private int pageNum;

    public BitbucketSynchronizeActivityMessage(Repository repository,
                                               Progress progress,
                                               boolean softSync,
                                               int pageNum,
                                               Set<Integer> processedPullRequests,
                                               Set<Integer> processedPullRequestsLocal)
    {
        this.repository = repository;
        this.progress = progress;
        this.softSync = softSync;
        this.pageNum = pageNum;
        this.processedPullRequests = processedPullRequests;
        this.processedPullRequestsLocal = processedPullRequestsLocal;
    }

    public BitbucketSynchronizeActivityMessage(Repository repository, boolean softSync)
    {
        this(repository, null, softSync, 1, new HashSet<Integer>(), new HashSet<Integer>());
    }

    @Override
    public Progress getProgress()
    {
        return progress;
    }

    public Repository getRepository()
    {
        return repository;
    }

    public boolean isSoftSync()
    {
        return softSync;
    }

    public Set<Integer> getProcessedPullRequests()
    {
        return processedPullRequests;
    }

    public int getPageNum()
    {
        return pageNum;
    }

    public Set<Integer> getProcessedPullRequestsLocal()
    {
        return processedPullRequestsLocal;
    }

}
