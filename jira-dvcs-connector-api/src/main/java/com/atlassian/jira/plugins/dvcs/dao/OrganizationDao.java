package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.model.Organization;

import java.util.Collection;
import java.util.List;

public interface OrganizationDao
{
    /**
     * returns all organizations
     * @return list of organizations
     */
    List<Organization> getAll();
    
    /**
     * @return returns count of all organizations
     */
    int getAllCount();

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
	 * Gets the all by ids.
	 *
	 * @param ids the ids
	 * @return the all by ids
	 */
	List<Organization> getAllByIds(Collection<Integer> ids);

	/**
	 * Gets the all by type.
	 *
	 * @param type the type
	 * @return the all
	 */
	List<Organization> getAllByType(String type);

	void setDefaultGroupsSlugs(int orgId, Collection<String> groupsSlugs);
	
	Organization findIntegratedAccount();

    boolean existsOrganizationWithType(String... types);
}
