package com.atlassian.jira.plugins.dvcs.model.dev;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RestPRRepository extends RestRepository
{
    private List<RestPullRequest> pullRequests;

    public RestPRRepository()
    {
    }

    public List<RestPullRequest> getPullRequests()
    {
        return pullRequests;
    }

    public void setPullRequests(final List<RestPullRequest> pullRequests)
    {
        this.pullRequests = pullRequests;
    }
}
