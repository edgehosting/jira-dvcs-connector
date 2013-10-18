package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * 
 * <pre>
 *               "user": {
 *                   "username": "erik",
 *                   "links": {
 *                      "self": {
 *                          "links": "https://bitbucket.org/api/1.0/users/erik"
 *                        },
 *                      "html": {
 *                          "links": "https://bitbucket.org/erik"
 *                        }
 *                   },
 *                   "display_name": "Erik van Zĳst"
 *               }
 * </pre>
 * 
 */
public class BitbucketPullRequestCommitAuthorInfo implements Serializable
{
    private static final long serialVersionUID = 8025642439790445876L;

    private String username;
    private BitbucketPullRequestLinks links;
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

    public BitbucketPullRequestLinks getLinks()
    {
        return links;
    }

    public void setLinks(BitbucketPullRequestLinks links)
    {
        this.links = links;
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
