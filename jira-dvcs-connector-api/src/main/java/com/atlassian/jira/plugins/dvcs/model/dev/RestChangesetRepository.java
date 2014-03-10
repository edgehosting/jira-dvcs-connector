package com.atlassian.jira.plugins.dvcs.model.dev;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class RestChangesetRepository extends RestRepository
{
    private List<RestChangeset> commits;

    public RestChangesetRepository()
    {
    }

    public List<RestChangeset> getCommits()
    {
        return commits;
    }

    public void setCommits(final List<RestChangeset> commits)
    {
        this.commits = commits;
    }

}
