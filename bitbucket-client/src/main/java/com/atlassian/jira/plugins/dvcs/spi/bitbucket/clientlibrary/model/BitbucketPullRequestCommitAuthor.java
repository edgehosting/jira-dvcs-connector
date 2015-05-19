package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
*
* <pre>
*     "author": {
*               "raw": "Erik van Zijst &lt;erik.van.zijst@gmail.com&gt;",
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
	
	private BitbucketUser user;

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

    public BitbucketUser getUser()
    {
        return user;
    }

    public void setUser(BitbucketUser user)
    {
        this.user = user;
    }
    
}
