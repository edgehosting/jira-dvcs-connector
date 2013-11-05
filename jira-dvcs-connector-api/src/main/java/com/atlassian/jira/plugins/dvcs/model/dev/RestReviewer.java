package com.atlassian.jira.plugins.dvcs.model.dev;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestReviewer
{
    private RestUser user;
    private boolean approved;
    private String role;

    public RestReviewer()
    {
    }

    public RestReviewer(final RestUser user, final boolean approved, final String role)
    {
        this.user = user;
        this.approved = approved;
        this.role = role;
    }

    public RestUser getUser()
    {
        return user;
    }

    public void setUser(final RestUser user)
    {
        this.user = user;
    }

    public boolean isApproved()
    {
        return approved;
    }

    public void setApproved(final boolean approved)
    {
        this.approved = approved;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(final String role)
    {
        this.role = role;
    }
}
