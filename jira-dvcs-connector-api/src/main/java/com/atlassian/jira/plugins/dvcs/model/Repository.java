package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Date;

public class Repository
{
    private final int id;
    private final int organizationId;
    private final String dvcsType;
    private final String name;
    private final String humanName;
    private final Date lastCommitDate;
    private final boolean linked;

    public Repository(int id, int organizationId, String dvcsType, String name, String humanName, Date lastCommitDate, boolean linked)
    {
        this.id = id;
        this.organizationId = organizationId;
        this.dvcsType = dvcsType;
        this.name = name;
        this.humanName = humanName;
        this.lastCommitDate = lastCommitDate;
        this.linked = linked;
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

    public String getName()
    {
        return name;
    }

    public String getHumanName()
    {
        return humanName;
    }

    public Date getLastCommitDate()
    {
        return lastCommitDate;
    }

    public boolean isLinked()
    {
        return linked;
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
                .append(name, this.name)
                .append(humanName, this.humanName)
                .append(lastCommitDate, this.lastCommitDate)
                .append(linked, this.linked)
                .isEquals();
	}

	@Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                .append(organizationId)
                .append(dvcsType)
                .append(name)
                .append(humanName)
                .append(lastCommitDate)
                .append(linked)
                .toHashCode();
    }


}
