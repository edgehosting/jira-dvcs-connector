package com.atlassian.jira.plugins.dvcs.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "accountinfo")
@XmlAccessorType(XmlAccessType.FIELD)
public class AccountInfo
{
    @XmlAttribute
    private String dvcsType;

    @XmlAttribute
    private boolean requiresOauth;

    public AccountInfo()    {}
    
    public AccountInfo(String dvcsType)
    {
        this(dvcsType, false);
    }

    public AccountInfo(String dvcsType, boolean requiresOauth)
    {
        this.dvcsType = dvcsType;
        this.requiresOauth = requiresOauth;
    }

    public String getDvcsType()
    {
        return dvcsType;
    }

    public boolean isRequiresOauth()
	{
		return requiresOauth;
	}

}
