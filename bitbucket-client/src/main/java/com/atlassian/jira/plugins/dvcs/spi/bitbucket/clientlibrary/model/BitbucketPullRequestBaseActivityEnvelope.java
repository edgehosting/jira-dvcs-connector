package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.List;

public class BitbucketPullRequestBaseActivityEnvelope implements Serializable
{
    private static final long serialVersionUID = -4076516797342633690L;
    private List<BitbucketPullRequestActivityInfo> values;
    private Integer pagelen;
    private Integer size;
    private String next;

    public List<BitbucketPullRequestActivityInfo> getValues()
    {
        return values;
    }

    public void setValues(List<BitbucketPullRequestActivityInfo> values)
    {
        this.values = values;
    }

    public Integer getPagelen()
    {
        return pagelen;
    }

    public void setPagelen(Integer pagelen)
    {
        this.pagelen = pagelen;
    }

    public Integer getSize()
    {
        return size;
    }

    public void setSize(Integer size)
    {
        this.size = size;
    }

	public String getNext() {
		return next;
	}

	public void setNext(String next) {
		this.next = next;
	}
}

