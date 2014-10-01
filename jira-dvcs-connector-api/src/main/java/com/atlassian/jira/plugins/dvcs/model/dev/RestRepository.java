package com.atlassian.jira.plugins.dvcs.model.dev;

import com.google.gson.annotations.SerializedName;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RestRepository
{
    private String name;
    private String slug;
    private String url;
    private String avatar;
    private Boolean fork;
    @SerializedName("forkOf")
    private RestRepository forkOf;

    public RestRepository()
    {
    }

    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
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

    public Boolean isFork()
    {
        return fork;
    }

    public void setFork(final boolean fork)
    {
        this.fork = fork;
    }

    public RestRepository getForkOf()
    {
        return forkOf;
    }

    public void setForkOf(final RestRepository forkOf)
    {
        this.forkOf = forkOf;
    }

    public String getSlug()
    {
        return slug;
    }

    public void setSlug(final String slug)
    {
        this.slug = slug;
    }
}
