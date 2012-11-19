package com.atlassian.jira.plugins.dvcs.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
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
    private String lastChangesetNode;
	private boolean linked;
    private boolean deleted;
    private boolean smartcommitsEnabled;
    
    private String repositoryUrl;
    
	private transient Credential credential;
	private transient String orgHostUrl;
	private transient String orgName;
	
	@XmlElement
    private DefaultProgress sync;

	public Repository()
	{
		super();
	}

	public Repository(int id, int organizationId, String dvcsType, String slug, String name,
			String lastChangesetNode, boolean linked, boolean deleted, Credential credential)
	{
		this.id = id;
		this.organizationId = organizationId;
		this.dvcsType = dvcsType;
		this.slug = slug;
		this.name = name;
        this.lastChangesetNode = lastChangesetNode;
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

    public String getLastChangesetNode() {
        return lastChangesetNode;
    }

    public void setLastChangesetNode(String lastChangesetNode) {
        this.lastChangesetNode = lastChangesetNode;
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
    
	public String getOrgHostUrl()
	{
		return orgHostUrl;
	}

	public void setOrgHostUrl(String orgHostUrl)
	{
		this.orgHostUrl = orgHostUrl;
	}

	public String getOrgName()
	{
		return orgName;
	}

	public void setOrgName(String orgName)
	{
		this.orgName = orgName;
	}

	public Progress getSync()
	{
		return sync;
	}

	public void setSync(DefaultProgress sync)
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
                .append(organizationId, that.organizationId)
                .append(dvcsType, that.dvcsType)
                .append(slug, that.slug)
                .append(name, that.name)
                .append(linked, that.linked)
                .append(deleted, that.deleted)
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
                .append(linked)
                .append(deleted)
                .toHashCode();
    }

	public String getRepositoryUrl()
	{
		return repositoryUrl;
	}

	public void setRepositoryUrl(String repositoryUrl)
	{
		this.repositoryUrl = repositoryUrl;
	}

	public boolean isSmartcommitsEnabled()
	{
		return smartcommitsEnabled;
	}

	public void setSmartcommitsEnabled(boolean smartcommitsEnabled)
	{
		this.smartcommitsEnabled = smartcommitsEnabled;
	}

	@Override
	public String toString()
	{
        return repositoryUrl + ", " + name + ", " + linked + ", " + deleted + ", " + smartcommitsEnabled;
	}
}
