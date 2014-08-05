package com.atlassian.jira.plugins.dvcs.model;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "smartCommitError")
@XmlAccessorType(XmlAccessType.FIELD)
public class SmartCommitError implements Serializable
{

    @XmlAttribute
    private String shortChangesetNode;
    @XmlAttribute
    private String commitUrl;
    @XmlAttribute
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
