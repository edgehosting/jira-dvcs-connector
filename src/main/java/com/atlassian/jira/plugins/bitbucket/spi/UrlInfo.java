package com.atlassian.jira.plugins.bitbucket.spi;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "urlinfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class UrlInfo
{
    @XmlAttribute
    private String repositoryType;

    @XmlAttribute
    private boolean isPrivate;

    @XmlAttribute
    private String repositoryUrl;
    
    @XmlList
    private final List<String> validationErrors =  new ArrayList<String>();

    @XmlAttribute
    private String projectKey;
    

    public UrlInfo() {}
    
    public UrlInfo(String repositoryType, boolean isPrivate, String repositoryUrl, String projectKey)
    {
        this.repositoryType = repositoryType;
        this.isPrivate = isPrivate;
        this.repositoryUrl = repositoryUrl;
        this.projectKey = projectKey;
    }
    
    public UrlInfo addValidationError(String validationError)
    {
        this.validationErrors.add(validationError);
        return this;
    }

    public String getRepositoryType()
    {
        return repositoryType;
    }

    public boolean isPrivate()
    {
        return isPrivate;
    }

    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }
    
    public List<String> getValidationErrors()
    {
        return validationErrors;
    }

    public String getProjectKey()
    {
        return projectKey;
    }
}
