package com.atlassian.jira.plugins.dvcs.dao;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Organization;

public interface OrganizationDao
{
    /**
     * returns all organizations
     * @return list of organizations
     */
    List<Organization> getAll();

    /**
     * returns Organization by ID
     *
     * @param organizationId id
     * @return organization
     */
    Organization get(int organizationId);

    /**
     * returns Organization by hostUrl and name
     *
     * @param hostUrl hostUrl
     * @param name name
     * @return organization
     */
    Organization getByHostAndName(String hostUrl, String name);

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
	 * @param plaintextPassword the plaintext password, null safe
	 * @param accessToken the access token, null safe
	 */
	void updateCredentials(int organizationId, String plaintextPassword,
			String accessToken);


}
