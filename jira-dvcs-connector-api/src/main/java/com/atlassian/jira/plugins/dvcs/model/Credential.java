package com.atlassian.jira.plugins.dvcs.model;

public class Credential
{
    private final String adminUsername;
    private final String adminPassword;
    private final String accessToken;

    public Credential(String adminUsername, String adminPassword, String accessToken)
    {
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.accessToken = accessToken;
    }

    public String getAdminUsername()
    {
        return adminUsername;
    }

    public String getAdminPassword()
    {
        return adminPassword;
    }

    public String getAccessToken()
    {
        return accessToken;
    }
}
