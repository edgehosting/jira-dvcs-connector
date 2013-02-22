package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * BitbucketPullRequestBaseActivity - JSON model
 *
 * 
 * 
 * <br /><br />
 * Created on 11.12.2012, 14:02:57
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketPullRequestBaseActivity implements Serializable, HasMessages
{

    private static final long serialVersionUID = -4076516797342633690L;
   
    // pull-request/2215/pull-request-api-1/diff
    // sometimes some of these dates are set

    private Date updatedOn;
    private Date createdOn;
    private Date date;
    
    private BitbucketActivityUser user;
    
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

    public BitbucketActivityUser getUser()
    {
        return user;
    }

    public void setUser(BitbucketActivityUser user)
    {
        this.user = user;
    }

    @Override
    public List<String> getMessages()
    {
        return new ArrayList<String>();
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

