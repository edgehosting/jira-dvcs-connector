package com.atlassian.jira.plugins.dvcs.github.api.model;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * A GitHub repository.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@XmlRootElement
@XmlType(propOrder = { "owner", "name" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepository
{

    /**
     * @see #getOwner()
     */
    private GitHubUser owner;

    /**
     * @see #getName()
     */
    private String name;

    /**
     * Constructor.
     */
    public GitHubRepository()
    {
    }

    /**
     * @return owner of repository
     */
    public GitHubUser getOwner()
    {
        return owner;
    }

    /**
     * @param owner
     *            {@link #getOwner()}
     */
    public void setOwner(GitHubUser owner)
    {
        this.owner = owner;
    }

    /**
     * @return name of repository
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name
     *            {@link #setName(String)}
     */
    public void setName(String name)
    {
        this.name = name;
    }

}
