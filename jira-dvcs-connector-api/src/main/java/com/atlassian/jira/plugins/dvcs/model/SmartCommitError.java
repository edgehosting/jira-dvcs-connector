package com.atlassian.jira.plugins.dvcs.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SmartCommitError
{

    private String shortChangesetNode;
    private String commitUrl;
    private String error;

    public SmartCommitError()
    {
    }

    public SmartCommitError(String changesetNode, String commitUrl, String error)
    {
        this.shortChangesetNode = changesetNode.substring(0, 7);
        this.commitUrl = commitUrl;
        this.error = error;
    }

    public String getShortChangesetNode()
    {
        return shortChangesetNode;
    }

    public void setShortChangesetNode(String shortChangesetNode)
    {
        this.shortChangesetNode = shortChangesetNode;
    }

    public String getCommitUrl()
    {
        return commitUrl;
    }

    public void setCommitUrl(String commitUrl)
    {
        this.commitUrl = commitUrl;
    }

    public String getError()
    {
        return error;
    }

    public void setError(String error)
    {
        this.error = error;
    }
}
