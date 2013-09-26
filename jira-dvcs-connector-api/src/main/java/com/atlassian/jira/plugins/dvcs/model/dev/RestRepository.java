package com.atlassian.jira.plugins.dvcs.model.dev;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RestRepository
{
    private int id;
    private String slug;
    private String name;
    private String scmId;
//    private String state;
    private String statusMessage;
//    private boolean forkable;
    private RestOrganization organization;
    private String url;
    private String avatar;

    public RestRepository()
    {
    }

    public int getId()
    {
        return id;
    }

    public void setId(final int id)
    {
        this.id = id;
    }

    public String getSlug()
    {
        return slug;
    }

    public void setSlug(final String slug)
    {
        this.slug = slug;
    }

    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public String getScmId()
    {
        return scmId;
    }

    public void setScmId(final String scmId)
    {
        this.scmId = scmId;
    }

    public String getStatusMessage()
    {
        return statusMessage;
    }

    public void setStatusMessage(final String statusMessage)
    {
        this.statusMessage = statusMessage;
    }

    public RestOrganization getOrganization()
    {
        return organization;
    }

    public void setOrganization(final RestOrganization organization)
    {
        this.organization = organization;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(final String url)
    {
        this.url = url;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(final String avatar)
    {
        this.avatar = avatar;
    }
}
