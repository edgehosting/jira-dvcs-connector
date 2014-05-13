package com.atlassian.jira.plugins.dvcs.model;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A list of repositories
 */
@XmlRootElement(name = "repositories")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepositoryList
{
    private List<Repository> repositories;

    public RepositoryList()
    {
    }

    public RepositoryList(List<Repository> list)
    {
        repositories = list;
    }

    public List<Repository> getRepositories()
    {
        return repositories;
    }

    public void setRepositories(List<Repository> repositories)
    {
        this.repositories = repositories;
    }
}

