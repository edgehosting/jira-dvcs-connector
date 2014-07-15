package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.Table;

@Preload
@Table("OrganizationMapping")
public interface OrganizationMapping extends Entity
{

    public static final String HOST_URL = "HOST_URL";
    public static final String NAME = "NAME";
    public static final String DVCS_TYPE = "DVCS_TYPE";
    public static final String AUTOLINK_NEW_REPOS = "AUTOLINK_NEW_REPOS";
    @Deprecated
    public static final String ADMIN_USERNAME = "ADMIN_USERNAME";
    @Deprecated
    public static final String ADMIN_PASSWORD = "ADMIN_PASSWORD";
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String SMARTCOMMITS_FOR_NEW_REPOS = "SMARTCOMMITS_FOR_NEW_REPOS";
    public static final String DEFAULT_GROUPS_SLUGS = "DEFAULT_GROUPS_SLUGS"; // serialized, separated by ";"

    public static final String OAUTH_KEY = "OAUTH_KEY";
    public static final String OAUTH_SECRET = "OAUTH_SECRET";

    String getHostUrl();
    String getName();
    @Indexed
    String getDvcsType();
    boolean isAutolinkNewRepos();
    @Deprecated
    String getAdminUsername();
    @Deprecated
    String getAdminPassword();
    String getAccessToken();
    boolean isSmartcommitsForNewRepos();
    String getDefaultGroupsSlugs();

    String getOauthKey();
    String getOauthSecret();

    void setHostUrl(String hostUrl);
    void setName(String name);
    void setDvcsType(String dvcsType);
    void setAutolinkNewRepos(boolean autolinkNewRepos);
    @Deprecated
    void setAdminUsername(String adminUsername);
    @Deprecated
    void setAdminPassword(String adminPassword);
    void setAccessToken(String accessToken);
    void setSmartcommitsForNewRepos(boolean enabled);
    void setDefaultGroupsSlugs(String slugs);

    void setOauthKey(String key);
    void setOauthSecret(String secret);
}
