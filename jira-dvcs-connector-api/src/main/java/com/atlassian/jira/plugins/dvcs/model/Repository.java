package com.atlassian.jira.plugins.dvcs.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@XmlRootElement
public class Repository
{
	private int id;
	private int organizationId;
	private String dvcsType;
	private String slug;
	private String name;
	private Date lastCommitDate;
	private boolean linked;
	private Credential credential;
	private SyncProgress sync;

	public Repository()
	{
		super();
	}

	public Repository(int id, int organizationId, String dvcsType, String slug, String name, Date lastCommitDate,
			boolean linked, Credential credential)
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
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (this.getClass() != obj.getClass())
			return false;
		Repository that = (Repository) obj;
		return new EqualsBuilder().append(organizationId, organizationId).append(dvcsType, dvcsType).append(slug, slug)
				.append(name, name).append(lastCommitDate, lastCommitDate).append(linked, linked)
				.append(credential, credential).isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder(17, 37).append(organizationId).append(dvcsType).append(slug).append(name)
				.append(lastCommitDate).append(linked).append(credential).toHashCode();
	}

	public SyncProgress getSync()
	{
		return sync;
	}

	public void setSync(SyncProgress sync)
	{
		this.sync = sync;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public void setOrganizationId(int organizationId)
	{
		this.organizationId = organizationId;
	}

	public void setDvcsType(String dvcsType)
	{
		this.dvcsType = dvcsType;
	}

	public void setSlug(String slug)
	{
		this.slug = slug;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setLastCommitDate(Date lastCommitDate)
	{
		this.lastCommitDate = lastCommitDate;
	}

	public void setLinked(boolean linked)
	{
		this.linked = linked;
	}

	public void setCredential(Credential credential)
	{
		this.credential = credential;
	}

}
