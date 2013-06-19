package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.util.List;

public class BitbucketChangesetPage
{
    private int pagelen;
    private String previous;
    private String next;
    private int page;
    private List<BitbucketNewChangeset> values;

    public int getPagelen()
    {
        return pagelen;
    }

    public void setPagelen(int pagelen)
    {
        this.pagelen = pagelen;
    }

    public String getPrevious()
    {
        return previous;
    }

    public void setPrevious(String previous)
    {
        this.previous = previous;
    }

    public String getNext()
    {
        return next;
    }

    public void setNext(String next)
    {
        this.next = next;
    }

    public int getPage()
    {
        return page;
    }

    public void setPage(int page)
    {
        this.page = page;
    }

    public List<BitbucketNewChangeset> getValues()
    {
        return values;
    }

    public void setValues(List<BitbucketNewChangeset> values)
    {
        this.values = values;
    }
}
