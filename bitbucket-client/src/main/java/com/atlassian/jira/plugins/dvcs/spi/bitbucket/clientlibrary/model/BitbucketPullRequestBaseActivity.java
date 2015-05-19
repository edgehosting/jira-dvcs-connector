package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.Date;

public class BitbucketPullRequestBaseActivity implements Serializable
{

    private static final long serialVersionUID = -4076516797342633690L;
   
    // pull-request/2215/pull-request-api-1/diff
    // sometimes some of these dates are set

    private Date updatedOn;
    private Date createdOn;
    private Date date;
    
    private BitbucketUser user;
    
    public BitbucketPullRequestBaseActivity()
    {
        super();
    }
    
    public Date getUpdatedOn()
    {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn)
    {
        this.updatedOn = updatedOn;
    }

    public BitbucketUser getUser()
    {
        return user;
    }

    public void setUser(BitbucketUser user)
    {
        this.user = user;
    }

    public Date getCreatedOn()
    {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn)
    {
        this.createdOn = createdOn;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }


}

