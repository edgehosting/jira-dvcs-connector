package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Date;

public class Repository
{
    private final int id;
    private final int organizationId;
    private final String dvcsType;
    private final String slug;
    private final String name;
    private final Date lastCommitDate;
    private final boolean linked;
    private Credential credential;

    public Repository(int id, int organizationId, String dvcsType, String slug, String name, Date lastCommitDate, boolean linked, Credential credential)
    {
        this.id = id;
        this.organizationId = organizationId;
        this.dvcsType = dvcsType;
        this.slug = name;
        this.name = name;
        this.lastCommitDate = lastCommitDate;
        this.linked = linked;
        this.credential = credential;
    }

    public int getId()
    {
        return id;
    }

    public int getOrganizationId()
    {
        return organizationId;
    }

    public String getDvcsType()
    {
        return dvcsType;
    }

    public String getSlug()
    {
        return slug;
    }

    public String getName()
    {
        return name;
    }

    public Date getLastCommitDate()
    {
        return lastCommitDate;
    }

    public boolean isLinked()
    {
        return linked;
    }

    public Credential getCredential()
    {
        return credential;
    }

    @Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (this==obj) return true;
		if (this.getClass()!=obj.getClass()) return false;
		Repository that = (Repository) obj;
		return new EqualsBuilder()
                .append(organizationId, this.organizationId)
                .append(dvcsType, this.dvcsType)
                .append(slug, this.slug)
                .append(name, this.name)
                .append(lastCommitDate, this.lastCommitDate)
                .append(linked, this.linked)
                .append(credential, this.credential)
                .isEquals();
	}

	@Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                .append(organizationId)
                .append(dvcsType)
                .append(slug)
                .append(name)
                .append(lastCommitDate)
                .append(linked)
                .append(credential)
                .toHashCode();
    }


}
