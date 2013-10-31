package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * BitbucketUser
 *
 * {
 *   "username": "jdoe",
 *   "display_name": "John Doe",
 *   "links": [...]
 * }
 *
 * @author miroslavstencel
 *
 */
public class BitbucketUser implements Serializable
{
    private static final long serialVersionUID = -817714326514194936L;

    private String username;
    private String displayName;
    private BitbucketLinks links;

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public BitbucketLinks getLinks()
    {
        return links;
    }

    public void setLinks(final BitbucketLinks links)
    {
        this.links = links;
    }
}
