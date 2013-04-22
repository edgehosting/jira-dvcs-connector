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

    public AccountInfo()    {}
    
    public AccountInfo(String dvcsType)
    {
        this.dvcsType = dvcsType;
    }

    public String getDvcsType()
    {
        return dvcsType;
    }
}
