package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;

import java.util.Collection;
import java.util.List;

/**
 * The Interface OrganizationService.
 */
public interface OrganizationService
{

    /**
     * check if account exists on given server using all available communicators.
     * @param hostUrl server host name
     * @param accountName name of account (organizationName)
     * @return accoutnInfo
     */
    AccountInfo getAccountInfo(String hostUrl, String accountName);

    /**
     * check if account exists on given server using communicator
     * of given <code>dvcsType</code>.
     * @param hostUrl server host name
     * @param accountName name of account (organizationName)
     * @param dvcsType type of DVCS
     * @return accoutnInfo
     */
    AccountInfo getAccountInfo(String hostUrl, String accountName, String dvcsType);

    /**
     * returns all organizations.
     *
     * @param loadRepositories the load repositories
     * @return list of organizations
     */
    List<Organization> getAll(boolean loadRepositories);
    
    /**
     * @return returns count of all organizations
     */
    int getAllCount();

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
	 * Update credentials
	 * 
     * @param organizationId
     * @param credential
     */
    public void updateCredentials(int organizationId, Credential credential);

    
    /**
     * @param organizationId
     * @param accessToken
     */
    public void updateCredentialsAccessToken(int organizationId, String accessToken);

	/**
	 * Enable autolink new repos.
	 *
	 * @param orgId the org id
	 * @param autolink the parse boolean
	 */
	void enableAutolinkNewRepos(int orgId, boolean autolink);

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
	 * Enable smartcommits on new repos.
	 *
	 * @param id the id
	 * @param parseBoolean the parse boolean
	 */
	void enableSmartcommitsOnNewRepos(int id, boolean parseBoolean);

	void setDefaultGroupsSlugs(int orgId, Collection<String> groupsSlugs);

	Organization findIntegratedAccount();

	Organization getByHostAndName(final String hostUrl, final String name);
    /**
     * Returns remote user who is owner of currently used accessToken
     * 
     * @param organizationId
     * @return
     */
    DvcsUser getTokenOwner(int organizationId);
    
    /**
     * @param organization
     * @return returns {@link Group}-s available for provided {@link Organization}
     */
    List<Group> getGroupsForOrganization(Organization organization);

    boolean existsOrganizationWithType(String... types);
}

