package com.atlassian.jira.plugins.dvcs.model.dev;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestOrganization
{
    private int id;
    private String name;
    private String dvcsType;

    public RestOrganization()
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

    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public String getDvcsType()
    {
        return dvcsType;
    }

    public void setDvcsType(final String dvcsType)
    {
        this.dvcsType = dvcsType;
    }
}
