package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

/**
 * "html": {
 *      "href": "https://bitbucket.org/user/repo"
 *  },
 */
public class BitbucketLink
{
    private String href;

    public String getHref()
    {
        return href;
    }

    public void setHref(String href)
    {
        this.href = href;
    }
}
