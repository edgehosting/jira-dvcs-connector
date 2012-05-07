package com.atlassian.jira.plugins.dvcs.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@XmlRootElement(name = "repository")
@XmlAccessorType(XmlAccessType.FIELD)
public class Repository
{
	private int id;
	private int organizationId;
	private String dvcsType;
	private String slug;
	private String name;
	private Date lastCommitDate;
	private boolean linked;
    private boolean deleted;
    
	private transient Credential credential;
	
    private SyncProgress sync;

	public Repository()
	{
		super();
	}

	public Repository(int id, int organizationId, String dvcsType, String slug, String name, Date lastCommitDate,
			boolean linked, boolean deleted, Credential credential)
	{
		this.id = id;
		this.organizationId = organizationId;
		this.dvcsType = dvcsType;
		this.slug = slug;
		this.name = name;
		this.lastCommitDate = lastCommitDate;
		this.linked = linked;
        this.deleted = deleted;
        this.credential = credential;
	}

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public int getOrganizationId()
    {
        return organizationId;
    }

    public void setOrganizationId(int organizationId)
    {
        this.organizationId = organizationId;
    }

    public String getDvcsType()
    {
        return dvcsType;
    }

    public void setDvcsType(String dvcsType)
    {
        this.dvcsType = dvcsType;
    }

    public String getSlug()
    {
        return slug;
    }

    public void setSlug(String slug)
    {
        this.slug = slug;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Date getLastCommitDate()
    {
        return lastCommitDate;
    }

    public void setLastCommitDate(Date lastCommitDate)
    {
        this.lastCommitDate = lastCommitDate;
    }

    public boolean isLinked()
    {
        return linked;
    }

    public void setLinked(boolean linked)
    {
        this.linked = linked;
    }

    public Credential getCredential()
    {
        return credential;
    }

    public void setCredential(Credential credential)
    {
        this.credential = credential;
    }

    public boolean isDeleted()
    {
        return deleted;
    }

    public void setDeleted(boolean deleted)
    {
        this.deleted = deleted;
    }
    
    public SyncProgress getSync()
    {
        return sync;
    }

    public void setSync(SyncProgress sync)
    {
        this.sync = sync;
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
                .append(deleted, this.deleted)
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
                .append(deleted)
                .append(credential)
                .toHashCode();
    }


}
