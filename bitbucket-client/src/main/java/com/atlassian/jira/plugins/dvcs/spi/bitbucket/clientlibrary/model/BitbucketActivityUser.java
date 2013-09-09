package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * BitbucketAccount represents both Bitbucket user and team.
 *
 * <pre>
 * "{
 *   "username": "jirabitbucketconnector",
 *   "display_name": "Jira DvcsConnector",
 *   "links": [
 *       {
 *           "href": "/api/1.0/users/jirabitbucketconnector",
 *           "rel": "self"
 *       },
 *       {
 *            "href": "/jirabitbucketconnector",
 *            "rel": "html"
 *       }
 *     ]
 * },
 * </pre>
 *
 */
public class BitbucketActivityUser implements Serializable
{
    private String username;
    private String displayName; 

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }
}
