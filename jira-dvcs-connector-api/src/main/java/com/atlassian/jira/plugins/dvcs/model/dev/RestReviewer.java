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

    public RestReviewer()
    {
    }

    public RestReviewer(final RestUser user, final boolean approved)
    {
        this.user = user;
        this.approved = approved;
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
}
