package com.atlassian.jira.plugins.dvcs.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Organization
{
    private int id;
    private String hostUrl;
    private String name;
    private String dvcsType;
    private boolean autolinkNewRepos;
    private boolean autoInviteNewUsers;
    
    private String organizationUrl;
    
    private List<Repository> repositories;
    
    private transient Credential credential;
    private transient List<Group> groups;
 
    public Organization()
	{
    	super();
	}
    
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

    public void setId(int id)
	{
		this.id = id;
	}

	public void setHostUrl(String hostUrl)
	{
		this.hostUrl = hostUrl;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setDvcsType(String dvcsType)
	{
		this.dvcsType = dvcsType;
	}

	public void setAutolinkNewRepos(boolean autolinkNewRepos)
	{
		this.autolinkNewRepos = autolinkNewRepos;
	}

	public void setCredential(Credential credential)
	{
		this.credential = credential;
	}

    public List<Repository> getRepositories()
    {
        return repositories;
    }

    public void setRepositories(List<Repository> repositories)
    {
        this.repositories = repositories;
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

	public boolean isAutoInviteNewUsers()
	{
		return autoInviteNewUsers;
	}

	public void setAutoInviteNewUsers(boolean autoInviteNewUsers)
	{
		this.autoInviteNewUsers = autoInviteNewUsers;
	}

	public List<Group> getGroups()
	{
		return groups;
	}

	public void setGroups(List<Group> groups)
	{
		this.groups = groups;
	}

	public String getOrganizationUrl()
	{
		return organizationUrl;
	}

	public void setOrganizationUrl(String organizationUrl)
	{
		this.organizationUrl = organizationUrl;
	}

}
