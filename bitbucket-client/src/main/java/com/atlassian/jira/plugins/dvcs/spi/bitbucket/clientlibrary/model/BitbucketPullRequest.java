package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.List;

/**
 * BitbucketPullRequest
 *
 * 
 * <br /><br />
 * Created on 11.12.2012, 14:02:57
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketPullRequest implements Serializable
{
    private static final long serialVersionUID = -4295609256398236631L;

    private Integer id;

    private String title;

    private String href;

    private BitbucketAccount user;
    
    private BitbucketPullRequestCommitInfo commits;
    
    // 
    private transient List<BitbucketPullRequestCommit> commitsDetails;
    
    public BitbucketPullRequest()
    {
        super();
    }

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public BitbucketAccount getUser()
    {
        return user;
    }

    public void setUser(BitbucketAccount user)
    {
        this.user = user;
    }

    public String getHref()
    {
        return href;
    }

    public void setHref(String href)
    {
        this.href = href;
    }

    public BitbucketPullRequestCommitInfo getCommits()
    {
        return commits;
    }

    public void setCommits(BitbucketPullRequestCommitInfo commits)
    {
        this.commits = commits;
    }

    public List<BitbucketPullRequestCommit> getCommitsDetails()
    {
        return commitsDetails;
    }

    public void setCommitsDetails(List<BitbucketPullRequestCommit> commitsDetails)
    {
        this.commitsDetails = commitsDetails;
    }
    

}

