package it.restart.com.atlassian.jira.plugins.dvcs.test;

public interface BasicTests
{
    void addOrganization();

    void addOrganizationWaitForSync();

    void addOrganizationInvalidAccount();
    
    void addOrganizationInvalidUrl();

    void addOrganizationInvalidOAuth();

    void testCommitStatistics();

    void testPostCommitHookAdded();

}
