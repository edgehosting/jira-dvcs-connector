package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;
import java.util.Date;

/**
 * BitbucketPullRequestBaseActivity - JSON model
 *
 * 
 * <br /><br />
 * Created on 11.12.2012, 14:02:57
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public abstract class BitbucketPullRequestBaseActivity implements Serializable, HasMessages
{

    private static final long serialVersionUID = -4076516797342633690L;
    
    private Date updatedOn;
    
    private BitbucketAccount user;
    
    private BitbucketRepository repository;
    
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

    public BitbucketAccount getUser()
    {
        return user;
    }

    public void setUser(BitbucketAccount user)
    {
        this.user = user;
    }

    public BitbucketRepository getRepository()
    {
        return repository;
    }

    public void setRepository(BitbucketRepository repository)
    {
        this.repository = repository;
    }


}

