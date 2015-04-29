package com.atlassian.jira.plugins.dvcs.pageobjects.common;

public class OAuthUtils
{
    public static final String TEST_OAUTH_PREFIX = "Test_OAuth_";

    public static String generateTestOAuthName() {
        return TEST_OAUTH_PREFIX + System.currentTimeMillis();
    }
}
