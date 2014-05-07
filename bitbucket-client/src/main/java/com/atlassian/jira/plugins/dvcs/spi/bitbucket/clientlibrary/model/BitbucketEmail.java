package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 * BitbucketEmail represents a single Email associated with a Bitbucket user account.
 *
 * <pre>
 *     {
 *          "active": true,
 *          "email": "2team.bb@gmail.com",
 *          "primary": true
 *     }
 * </pre>
 */
public class BitbucketEmail implements Serializable
{
    private boolean active;
    private String email;
    private boolean primary;

    public BitbucketEmail(boolean active, String email, boolean primary)
    {
        this.active = active;
        this.email = email;
        this.primary = primary;
    }

    public boolean isActive()
    {
        return active;
    }

    public String getEmail()
    {
        return email;
    }

    public boolean isPrimary()
    {
        return primary;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public void setPrimary(boolean primary)
    {
        this.primary = primary;
    }
}
