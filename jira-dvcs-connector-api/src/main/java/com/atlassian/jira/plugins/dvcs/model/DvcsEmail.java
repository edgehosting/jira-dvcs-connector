package com.atlassian.jira.plugins.dvcs.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Both Github and Bitbucket allow users to have multiple mapped e-mail addresses.
 *
 * This is a class representing an e-mail, primary state, active || verified state.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DvcsEmail
{
    private String email;
    private boolean primary;

    /*
     GitHub has verified state for e-mails. Bitbucket has active state.
     We'll consolidate on "active".
    */
    private boolean active;

    public DvcsEmail()
    {
    }

    public DvcsEmail(String email, boolean primary, boolean active)
    {
        this.email = email;
        this.primary = primary;
        this.active = active;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public boolean isPrimary()
    {
        return primary;
    }

    public void setPrimary(boolean primary)
    {
        this.primary = primary;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }
}
