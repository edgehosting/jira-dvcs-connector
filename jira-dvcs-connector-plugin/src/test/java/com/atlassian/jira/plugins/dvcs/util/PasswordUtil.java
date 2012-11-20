package com.atlassian.jira.plugins.dvcs.util;

public class PasswordUtil
{
    public static String getPassword(String username)
    {
        return System.getProperty(username + ".password");
    }
}
