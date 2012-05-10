package com.atlassian.jira.plugins.dvcs.service;

import java.util.Collection;
import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Organization;

/**
 * The Interface OrganizationService.
 */
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
     * returns all organizations.
     *
     * @param loadRepositories the load repositories
     * @return list of organizations
     */
    List<Organization> getAll(boolean loadRepositories);

    /**
     * returns Organization by ID.
     *
     * @param organizationId id
     * @param loadRepositories the load repositories
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
     * remove Organization from storage.
     *
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

	/**
	 * Update credentials access token.
	 *
	 * @param organizationId the organization id
	 * @param accessToken the access token
	 */
	void updateCredentialsAccessToken(int organizationId, String accessToken);

	/**
	 * Enable autolink new repos.
	 *
	 * @param orgId the org id
	 * @param autolink the parse boolean
	 */
	void enableAutolinkNewRepos(int orgId, boolean autolink);

	/**
	 * Enable auto invite users.
	 *
	 * @param id the id
	 * @param autoInviteUsers the auto invite users
	 */
	void enableAutoInviteUsers(int id, boolean autoInviteUsers);
	
	/**
	 * Gets the auto invition organizations.
	 *
	 * @return the auto invition organizations
	 */
	List<Organization> getAutoInvitionOrganizations();
	
	/**
	 * Gets the all by ids.
	 *
	 * @param ids the ids
	 * @return the all by ids
	 */
	List<Organization> getAllByIds(Collection<Integer> ids);

}
