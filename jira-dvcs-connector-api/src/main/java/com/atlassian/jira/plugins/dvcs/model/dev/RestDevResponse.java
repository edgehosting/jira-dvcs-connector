package com.atlassian.jira.plugins.dvcs.model.dev;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestDevResponse
{
    private List<? extends RestRepository> repositories;

    public RestDevResponse()
    {
    }

    public List<? extends RestRepository> getRepositories()
    {
        return repositories;
    }

    public void setRepositories(final List<? extends RestRepository> repositories)
    {
        this.repositories = repositories;
    }
}
