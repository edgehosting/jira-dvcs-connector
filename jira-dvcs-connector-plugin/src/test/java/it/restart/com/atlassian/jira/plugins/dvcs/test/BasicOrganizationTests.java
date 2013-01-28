package it.restart.com.atlassian.jira.plugins.dvcs.test;



public interface BasicOrganizationTests
{
    void addOrganization();

    void addOrganizationWaitForSync();

    void addOrganizationInvalidUrl();

    void addOrganizationInvalidOAuth();
}
