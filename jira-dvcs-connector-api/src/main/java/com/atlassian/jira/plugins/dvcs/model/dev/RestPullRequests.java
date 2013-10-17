package com.atlassian.jira.plugins.dvcs.model.dev;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 *
 */
@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestPullRequests
{
    private List<RestPRRepository> repositories;

    public List<RestPRRepository> getRepositories()
    {
        return repositories;
    }

    public void setRepositories(final List<RestPRRepository> repositories)
    {
        this.repositories = repositories;
    }
}
