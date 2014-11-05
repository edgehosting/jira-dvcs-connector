package it.restart.com.atlassian.jira.plugins.dvcs.test;

/**
 * Contains the Basic information for the test account that is re-used across our tests.
 */
public final class IntegrationTestUserDetails
{
    public static final String ACCOUNT_NAME = "jirabitbucketconnector";

    public static final String PASSWORD = System.getProperty("jirabitbucketconnector.password");
}
