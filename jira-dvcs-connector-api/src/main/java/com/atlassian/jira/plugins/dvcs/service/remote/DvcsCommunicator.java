package com.atlassian.jira.plugins.dvcs.service.remote;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public interface DvcsCommunicator
{
	String getDvcsType();

	AccountInfo getAccountInfo(String hostUrl, String accountName);

	List<Repository> getRepositories(Organization organization);

	Changeset getDetailChangeset(Repository repository, String node);

	Iterable<Changeset> getChangesets(Repository repository);

	void setupPostcommitHook(Repository repository, String postCommitUrl);

	void linkRepository(Repository repository, Set<String> withProjectkeys);

	void linkRepositoryIncremental(Repository repository, Set<String> withPossibleNewProjectkeys);

	void removePostcommitHook(Repository repository, String postCommitUrl);

	String getCommitUrl(Repository repository, Changeset changeset);

	String getFileCommitUrl(Repository repository, Changeset changeset, String file, int index);

	DvcsUser getUser(Repository repository, String username);

	String getUserUrl(Repository repository, Changeset changeset);

	boolean isOauthConfigured();

	// -----------------------------------------------------------------------
	// methods for invitation management on bitbucket
	// -----------------------------------------------------------------------
	/**
	 * In the meaning of
	 * <ul>
	 * <li>For bitbucket it is "groups"</li>
	 * <li>For github it is "organizations" in combination with "teams" (we are
	 * not going to use this)</li>
	 * </ul>
	 * .
	 * 
	 * @param organization
	 *            the organization
	 * @return list of groups
	 */
	Set<Group> getGroupsForOrganization(Organization organization);

	/**
	 * Supports invitation.
	 * 
	 * @param organization
	 *            the organization
	 * @return true, if supports invitation for given organization
	 */
	boolean supportsInvitation(Organization organization);

	/**
	 * Invite user.
	 * 
	 * @param organization
	 *            the organization
	 * @param groupSlugs
	 *            the group slugs
	 * @param userEmail
	 *            the user email
	 */
	void inviteUser(Organization organization, Collection<String> groupSlugs, String userEmail);

	// FIXME: consider refactor necessity

	void synchronize(Repository repository);

}
