package com.atlassian.jira.plugins.dvcs.service;

import java.util.Collection;
import java.util.List;

import com.atlassian.jira.plugins.dvcs.exception.InvalidCredentialsException;
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
     * @param username the username
     * @param plaintextPassword the password as a plain text
     */
    void updateCredentials(int organizationId, String username, String plaintextPassword);

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

	/**
	 * Gets the all.
	 *
	 * @param loadRepositories the load repositories
	 * @param type the type
	 * @return the all
	 */
	List<Organization> getAll(boolean loadRepositories, String type);
	
	/**
	 * Check credentials.
	 *
	 * @param forOrganization the for organization
	 * @throws InvalidCredentialsException the invalid credentials exception if credentials seems
	 * to be invalid
	 */
	void checkCredentials(Organization forOrganizationWithPlainCredentials) throws InvalidCredentialsException;

	/**
	 * Enable global smartcommits.
	 *
	 * @param id the id
	 * @param parseBoolean the parse boolean
	 */
	void enableGlobalSmartcommits(int id, boolean parseBoolean);
	
	void setDefaultGroupsSlugs(int orgId, Collection<String> groupsSlugs);
}

