package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("OrganizationMapping")
public interface OrganizationMapping extends Entity
{

    public static final String HOST_URL = "HOST_URL";
    public static final String NAME = "NAME";
    public static final String DVCS_TYPE = "DVCS_TYPE";
    public static final String ADMIN_USERNAME = "ADMIN_USERNAME";
    public static final String ADMIN_PASSWORD = "ADMIN_PASSWORD";
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";

    String getHostUrl();
    String getName();
    String getDvcsType();
    String getAdminUsername();
    String getAdminPassword();
    String getAccessToken();

    void setHostUrl(String hostUrl);
    void setName(String name);
    void setDvcsType(String dvcsType);
    void setAdminUsername(String adminUsername);
    void setAdminPassword(String adminPassword);
    void setAccessToken(String accessToken);
}
