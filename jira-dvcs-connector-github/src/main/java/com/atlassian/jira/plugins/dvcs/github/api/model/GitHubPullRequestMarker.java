package com.atlassian.jira.plugins.dvcs.github.api.model;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Marker for GitHub pull request base and appropriate head.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@XmlRootElement
@XmlType(propOrder = { "ref", "repo" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubPullRequestMarker
{

    /**
     * @see #getRef()
     */
    private String ref;

    /**
     * @see #getRepo()
     */
    private GitHubRepository repo;

    /**
     * Constructor.
     */
    public GitHubPullRequestMarker()
    {
    }

    /**
     * @return Git reference.
     */
    public String getRef()
    {
        return ref;
    }

    /**
     * @param ref
     *            {@link #getRef()}
     */
    public void setRef(String ref)
    {
        this.ref = ref;
    }

    /**
     * @return On which repository is this marker.
     */
    public GitHubRepository getRepo()
    {
        return repo;
    }

    /**
     * @param repo
     *            {@link #getRepo()}
     */
    public void setRepo(GitHubRepository repo)
    {
        this.repo = repo;
    }

}
