package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement (name = "repository")
@XmlAccessorType (XmlAccessType.FIELD)
public class Repository implements Serializable
{
    private int id;
    private int organizationId;
    private String dvcsType;
    private String slug;
    private String name;
    private Date lastCommitDate;
    private boolean linked;
    private boolean deleted;
    private boolean smartcommitsEnabled;
    private String owner;

    private Date activityLastSync;

    /**
     * Last activity date, either last commit or last PR activity
     */
    private Date lastActivityDate;

    private String repositoryUrl;
    private String logo;
    private boolean fork;
    private Repository forkOf;

    private transient Credential credential;
    private transient String orgHostUrl;
    private transient String orgName;

    @XmlElement
    private DefaultProgress sync;
    private transient boolean updateLinkAuthorised;

    public Repository()
    {
        super();
    }

    public Repository(int id, int organizationId, String dvcsType, String slug, String name, Date lastCommitDate,
            boolean linked, boolean deleted, Credential credential, boolean updateLinkAuthorised)
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
        this.updateLinkAuthorised = updateLinkAuthorised;
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

    public Date getLastActivityDate()
    {
        return lastActivityDate;
    }

    public void setLastActivityDate(Date lastActivityDate)
    {
        this.lastActivityDate = lastActivityDate;
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

    public String getLogo()
    {
        return logo;
    }

    public void setLogo(final String logo)
    {
        this.logo = logo;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) { return false; }
        if (this == obj) { return true; }
        if (this.getClass() != obj.getClass()) { return false; }
        Repository that = (Repository) obj;
        return new EqualsBuilder()
                .append(organizationId, that.organizationId)
                .append(dvcsType, that.dvcsType)
                .append(slug, that.slug)
                .append(name, that.name)
                .append(linked, that.linked)
                .append(deleted, that.deleted)
                .append(updateLinkAuthorised, that.updateLinkAuthorised)
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
                .append(updateLinkAuthorised)
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

    public boolean isFork()
    {
        return fork;
    }

    public void setFork(final boolean fork)
    {
        this.fork = fork;
    }

    public Repository getForkOf()
    {
        return forkOf;
    }

    public void setForkOf(final Repository forkOf)
    {
        this.forkOf = forkOf;
    }

    public String getOwner()
    {
        return owner;
    }

    public void setOwner(final String owner)
    {
        this.owner = owner;
    }

    @Override
    public String toString()
    {
        return repositoryUrl + ", " + name + ", " + linked + ", " + deleted + ", " + smartcommitsEnabled;
    }

    public Date getActivityLastSync()
    {
        return activityLastSync;
    }

    public void setActivityLastSync(Date activityLastSync)
    {
        this.activityLastSync = activityLastSync;
    }

    public void setUpdateLinkAuthorised(boolean updateLinkAuthorised)
    {
        this.updateLinkAuthorised = updateLinkAuthorised;
    }

    public boolean getUpdateLinkAuthorised()
    {
        return updateLinkAuthorised;
    }

}
