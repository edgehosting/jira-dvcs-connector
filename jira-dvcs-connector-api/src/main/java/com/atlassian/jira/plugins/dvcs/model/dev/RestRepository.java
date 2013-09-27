package com.atlassian.jira.plugins.dvcs.model.dev;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RestRepository
{
    private String name;
    private String url;
    private String avatar;
    private List<RestChangeset> commits;

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

    public List<RestChangeset> getCommits()
    {
        return commits;
    }

    public void setCommits(final List<RestChangeset> commits)
    {
        this.commits = commits;
    }
}
