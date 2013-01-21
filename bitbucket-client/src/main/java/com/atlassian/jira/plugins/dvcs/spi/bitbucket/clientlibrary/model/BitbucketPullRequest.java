package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

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

    private BitbucketAccount user;
    
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
    

}

