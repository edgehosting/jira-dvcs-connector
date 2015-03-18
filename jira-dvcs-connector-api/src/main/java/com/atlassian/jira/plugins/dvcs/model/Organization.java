package com.atlassian.jira.plugins.dvcs.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class Organization implements Serializable
{
    public static final String GROUP_SLUGS_SEPARATOR = ";";

    private int id;
    private String hostUrl;
    private String name;
    private String dvcsType;
    private boolean autolinkNewRepos;
    private boolean smartcommitsOnNewRepos;
    private String organizationUrl;
    private List<Repository> repositories;
    private transient Credential credential;

    // 2/ invitation groups - when adding new user as information holder for rendering form extension
    private transient List<Group> groups;

    // 1/ default groups - when configuring default groups
    private transient Set<Group> defaultGroups;

    public Organization()
    {
        super();
    }

    public Organization(int id, String hostUrl, String name, String dvcsType,
            boolean autolinkNewRepos, Credential credential, String organizationUrl,
            boolean smartcommitsOnNewRepos, Set<Group> defaultGroups)
    {
        this.id = id;
        this.hostUrl = hostUrl;
        this.name = name;
        this.dvcsType = dvcsType;
        this.autolinkNewRepos = autolinkNewRepos;
        this.credential = credential;
        this.organizationUrl = organizationUrl;
        this.smartcommitsOnNewRepos = smartcommitsOnNewRepos;
        this.defaultGroups = defaultGroups;
    }

    public Organization(Organization other)
    {
        this(other.id, other.hostUrl, other.name, other.dvcsType, other.autolinkNewRepos,
                new Credential(other.credential.getOauthKey(), other.credential.getOauthSecret(),
                        other.credential.getAccessToken(), other.credential.getAdminUsername(), other.credential.getAdminPassword()),
                other.organizationUrl, other.smartcommitsOnNewRepos, null);

        this.groups = other.groups != null ? Lists.newArrayList(other.groups) : null;
        this.defaultGroups = other.defaultGroups != null ? Sets.newHashSet(other.defaultGroups) : null;
        this.repositories = other.repositories != null ? Lists.newArrayList(other.repositories) : null;
    }

    // =============== getters ==========================
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

    public List<Group> getGroups()
    {
        return groups;
    }

    public String getOrganizationUrl()
    {
        return organizationUrl;
    }

    public Set<Group> getDefaultGroups()
    {
        return defaultGroups;
    }

    public boolean isSmartcommitsOnNewRepos()
    {
        return smartcommitsOnNewRepos;
    }

    // =============== setters ==========================
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

    public void setGroups(List<Group> groups)
    {
        this.groups = groups;
    }

    public void setOrganizationUrl(String organizationUrl)
    {
        this.organizationUrl = organizationUrl;
    }

    public void setDefaultGroups(Set<Group> defaultGroups)
    {
        this.defaultGroups = defaultGroups;
    }

    public void setSmartcommitsOnNewRepos(boolean smartcommitsOnNewRepos)
    {
        this.smartcommitsOnNewRepos = smartcommitsOnNewRepos;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

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

    public boolean isIntegratedAccount()
    {
        return credential != null && StringUtils.isNotBlank(credential.getOauthKey())
                && StringUtils.isNotBlank(credential.getOauthSecret()) && StringUtils.isBlank(credential.getAccessToken());
    }
}
