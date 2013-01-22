package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
*
* <pre>
*     "author": {
*               "raw": "Erik van Zijst <erik.van.zijst@gmail.com>",
*               "user": {
*                   "username": "erik",
*                   "href": "/api/1.0/users/erik",
*                   "display_name": "Erik van Zĳst"
*               }
*           }
* </pre>
* 
*/
public class BitbucketPullRequestCommitAuthor implements Serializable
{
	private static final long serialVersionUID = 8025642439790445876L;
    
	private String raw;
	
	private BitbucketPullRequestCommitAuthor user;

	public BitbucketPullRequestCommitAuthor()
    {
	    super();
    }

    public String getRaw()
    {
        return raw;
    }

    public void setRaw(String raw)
    {
        this.raw = raw;
    }

    public BitbucketPullRequestCommitAuthor getUser()
    {
        return user;
    }

    public void setUser(BitbucketPullRequestCommitAuthor user)
    {
        this.user = user;
    }
    
}
