package com.atlassian.jira.plugins.bitbucket.spi;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
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
    private String validationError;

    public UrlInfo() {}
    
    public UrlInfo(String repositoryType, boolean isPrivate, String validationError)
    {
        this.repositoryType = repositoryType;
        this.isPrivate = isPrivate;
        this.validationError = validationError;
    }

    public String getRepositoryType()
    {
        return repositoryType;
    }

    public boolean isPrivate()
    {
        return isPrivate;
    }

    public String getValidationError()
    {
        return validationError;
    }
    
}
