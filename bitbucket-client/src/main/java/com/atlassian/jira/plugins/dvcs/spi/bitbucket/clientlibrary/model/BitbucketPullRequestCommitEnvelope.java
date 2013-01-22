package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.List;

/**
 *
 * 
 * <br /><br />
 * Created on 11.12.2012, 14:02:57
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketPullRequestCommitEnvelope implements Serializable
{
    private static final long serialVersionUID = 8212352604704981087L;

    private List<BitbucketPullRequestCommit> values;

    private Integer size;

    private Integer pagelen;

    private Integer page;

    private String next;

    public BitbucketPullRequestCommitEnvelope()
    {
        super();
    }

    public List<BitbucketPullRequestCommit> getValues()
    {
        return values;
    }

    public void setValues(List<BitbucketPullRequestCommit> values)
    {
        this.values = values;
    }

    public Integer getSize()
    {
        return size;
    }

    public void setSize(Integer size)
    {
        this.size = size;
    }

    public Integer getPagelen()
    {
        return pagelen;
    }

    public void setPagelen(Integer pagelen)
    {
        this.pagelen = pagelen;
    }

    public Integer getPage()
    {
        return page;
    }

    public void setPage(Integer page)
    {
        this.page = page;
    }

    public String getNext()
    {
        return next;
    }

    public void setNext(String next)
    {
        this.next = next;
    }

  
}

