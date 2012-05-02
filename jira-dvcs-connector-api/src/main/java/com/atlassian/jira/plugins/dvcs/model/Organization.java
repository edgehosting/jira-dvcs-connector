package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class Organization
{
    private final int id;
    private final String hostUrl;
    private final String name;
    private final String dvcsType;
    private final boolean autolinkNewRepos;
    private final Credential credential;

    public Organization(int id, String hostUrl, String name, String dvcsType, boolean autolinkNewRepos, Credential credential)
    {
        this.id = id;
        this.hostUrl = hostUrl;
        this.name = name;
        this.dvcsType = dvcsType;
        this.autolinkNewRepos = autolinkNewRepos;
        this.credential = credential;
    }

    public int getId()
    {
        return id;
    }

    public String getHostUrl()
    {
        return hostUrl;
    }

    public String getName()
    {
        return name;
    }

    public String getDvcsType()
    {
        return dvcsType;
    }

    public boolean isAutolinkNewRepos()
    {
        return autolinkNewRepos;
    }

    public Credential getCredential()
    {
        return credential;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Organization that = (Organization) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .append(hostUrl, that.hostUrl)
                .append(name, that.name)
                .append(dvcsType, that.dvcsType)
                .append(credential, that.credential)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
                .append(id)
                .append(hostUrl)
                .append(name)
                .append(dvcsType)
                .append(credential)
                .hashCode();
    }

}
