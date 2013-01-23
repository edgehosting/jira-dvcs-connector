package it.restart.com.atlassian.jira.plugins.dvcs.test;


public interface BasicOrganizationTests
{
    void addOrganization();
//    public void addOrganizationFailingStep1(String url);
//    public void addRepoToProjectFailingStep2();
//    public void addRepoToProjectFailingPostcommitService(String url);
//    public void addOrganizationSuccessfully(String organizationAccount, boolean autosync);

    void addOrganizationWaitForSync();
    
    
}
