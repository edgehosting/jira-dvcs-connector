package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import java.io.Serializable;
import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;

public class BitbucketSynchronizeActivityMessage implements Serializable, HasProgress
{

    private static final long serialVersionUID = -4361088769277502144L;

    private boolean softSync;

    private String pageUrl;
    private Repository repository;
    private Progress progress;
    private List<Integer> processedPullRequests;

    public BitbucketSynchronizeActivityMessage(Repository repository,
                                               Progress progress,
                                               boolean softSync,
                                               String pageUrl,
                                               List<Integer> processedPullRequests)
    {
        this.repository = repository;
        this.progress = progress;
        this.softSync = softSync;
        this.pageUrl = pageUrl;
        this.processedPullRequests = processedPullRequests;
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

    public String getPageUrl()
    {
        return pageUrl;
    }

    public List<Integer> getProcessedPullRequests()
    {
        return processedPullRequests;
    }

}
