package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * <pre>
 * BitbucketAuthor
 * {
 *   "raw": "John Doe &lt;john.doe@gmail.com&gt;",
 *   "user": {...}
 * }
 * </pre>
 */
public class BitbucketAuthor implements Serializable
{
    private static final long serialVersionUID = -6345263554099823139L;

    private String raw;
    private BitbucketUser user;

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
