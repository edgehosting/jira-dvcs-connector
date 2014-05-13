package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "repositoryRegistration")
@XmlAccessorType(XmlAccessType.FIELD)
public class RepositoryRegistration
{
    private Repository repository;
    private String callBackUrl;
    private boolean callBackUrlInstalled;

    public Repository getRepository()
    {
        return repository;
    }

    public void setRepository(Repository repository)
    {
        this.repository = repository;
    }

    public String getCallBackUrl()
    {
        return callBackUrl;
    }

    public void setCallBackUrl(String callBackUrl)
    {
        this.callBackUrl = callBackUrl;
    }

    public boolean isCallBackUrlInstalled()
    {
        return callBackUrlInstalled;
    }

    public void setCallBackUrlInstalled(boolean callBackUrlInstalled)
    {
        this.callBackUrlInstalled = callBackUrlInstalled;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (this.getClass() != obj.getClass())
            return false;
        RepositoryRegistration that = (RepositoryRegistration) obj;
        return new EqualsBuilder().append(repository, that.repository).append(callBackUrl, that.callBackUrl).append(callBackUrl, that.callBackUrlInstalled)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(repository).append(callBackUrl).append(callBackUrlInstalled).toHashCode();
    }
}
