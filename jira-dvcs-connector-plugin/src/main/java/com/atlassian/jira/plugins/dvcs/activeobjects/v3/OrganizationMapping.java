package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Table;

@Preload
@Table("OrganizationMapping")
public interface OrganizationMapping extends Entity
{

    public static final String HOST_URL = "HOST_URL";
    public static final String NAME = "NAME";
    public static final String DVCS_TYPE = "DVCS_TYPE";
    public static final String AUTOLINK_NEW_REPOS = "AUTOLINK_NEW_REPOS";
    public static final String ADMIN_USERNAME = "ADMIN_USERNAME";
    public static final String ADMIN_PASSWORD = "ADMIN_PASSWORD";
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String SMARTCOMMITS_FOR_NEW_REPOS = "SMARTCOMMITS_FOR_NEW_REPOS";
    public static final String DEFAULT_GROUPS_SLUGS = "DEFAULT_GROUPS_SLUGS"; // serialized, separated by ";"

    String getHostUrl();
    String getName();
    String getDvcsType();
    boolean isAutolinkNewRepos();
    String getAdminUsername();
    String getAdminPassword();
    String getAccessToken();
    boolean isSmartcommitsForNewRepos();
    String getDefaultGroupsSlugs();

    void setHostUrl(String hostUrl);
    void setName(String name);
    void setDvcsType(String dvcsType);
    void setAutolinkNewRepos(boolean autolinkNewRepos);
    void setAdminUsername(String adminUsername);
    void setAdminPassword(String adminPassword);
    void setAccessToken(String accessToken);
    void setSmartcommitsForNewRepos(boolean enabled);
    void setDefaultGroupsSlugs(String slugs);
}
