package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * 
 * <pre>
 *               "user": {
 *                   "username": "erik",
 *                   "href": "/api/1.0/users/erik",
 *                   "display_name": "Erik van Zĳst"
 *               }
 * </pre>
 * 
 */
public class BitbucketPullRequestCommitAuthorInfo implements Serializable
{
    private static final long serialVersionUID = 8025642439790445876L;

    private String username;
    private String href;
    private String displayName;

    public BitbucketPullRequestCommitAuthorInfo()
    {
        super();
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getHref()
    {
        return href;
    }

    public void setHref(String href)
    {
        this.href = href;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

}
