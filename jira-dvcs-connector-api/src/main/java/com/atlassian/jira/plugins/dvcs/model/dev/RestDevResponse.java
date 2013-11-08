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
public class RestDevResponse<T extends  RestRepository>
{
    private List<T> repositories;

    public RestDevResponse()
    {
    }

    public List<T> getRepositories()
    {
        return repositories;
    }

    public void setRepositories(final List<T> repositories)
    {
        this.repositories = repositories;
    }
}
