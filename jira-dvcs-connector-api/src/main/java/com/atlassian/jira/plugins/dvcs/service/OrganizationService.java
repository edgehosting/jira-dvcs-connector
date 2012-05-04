package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Organization;

import java.util.List;

public interface OrganizationService
{

    /**
     * check if account exists on given server.
     * @param hostUrl server host name
     * @param accountName name of account (organizationName)
     * @return accoutnInfo
     */
    AccountInfo getAccountInfo(String hostUrl, String accountName);

    /**
     * returns all organizations
     * @return list of organizations
     * @param loadRepositories
     */
    List<Organization> getAll(boolean loadRepositories);

    /**
     * returns Organization by ID
     *
     * @param organizationId id
     * @param loadRepositories
     * @return organization
     */
    Organization get(int organizationId, boolean loadRepositories);

    /**
     * save Organization to storage. If it's new object (without ID) after this operation it will have it assigned.
     * @param organization organization
     * @return saved organization
     */
    Organization save(Organization organization);

    /**
     * remove Organization from storage
     * @param organizationId id
     */
    void remove(int organizationId);
    
    /**
     * Update credentials.
     *
     * @param organizationId the organization id
     * @param plaintextPassword the password as a plain text
     */
    void updateCredentials(int organizationId, String plaintextPassword);

}
