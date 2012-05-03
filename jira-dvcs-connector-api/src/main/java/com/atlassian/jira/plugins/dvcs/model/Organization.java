package com.atlassian.jira.plugins.dvcs.model;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@XmlRootElement
public class Organization
{
    private int id;
    private String hostUrl;
    private String name;
    private String dvcsType;
    private boolean autolinkNewRepos;
    private Credential credential;

    private Repository [] repositories;
 
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

	public Repository[] getRepositories()
	{
		return repositories;
	}

	public void setRepositories(Repository[] repositories)
	{
		this.repositories = repositories;
	}

}
