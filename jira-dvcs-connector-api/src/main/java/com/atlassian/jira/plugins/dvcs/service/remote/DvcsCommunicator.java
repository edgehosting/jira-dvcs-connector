package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetailsEnvelope;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public interface DvcsCommunicator
{
    String POST_HOOK_SUFFIX = String.valueOf("/rest/bitbucket/1.0/repository/");

    String getDvcsType();

	AccountInfo getAccountInfo(String hostUrl, String accountName);

	List<Repository> getRepositories(Organization organization, List<Repository> storedRepositories);
    
    List<Branch> getBranches(Repository repository);

    Changeset getChangeset(Repository repository, String node);

    /**
     * Gets the file details for a given changeset.
     *
     * @param repository the Repository
     * @param changeset the Changeset
     * @return ChangesetFileDetailsEnvelope
     * @throws com.atlassian.jira.plugins.dvcs.exception.SourceControlException
     */
    ChangesetFileDetailsEnvelope getFileDetails(Repository repository, Changeset changeset);

	void ensureHookPresent(Repository repository, String postCommitUrl);

    void linkRepository(Repository repository, Set<String> withProjectkeys);

	void removePostcommitHook(Repository repository, String postCommitUrl);

	String getCommitUrl(Repository repository, Changeset changeset);

	String getFileCommitUrl(Repository repository, Changeset changeset, String file, int index);

    DvcsUser getUser(Repository repository, String author);

    /**
     * Returns remote user who is owner of currently used accessToken
     *
     * @param organization
     * @return
     */
    DvcsUser getTokenOwner(Organization organization);

    //-----------------------------------------------------------------------
    // methods for invitation management on bitbucket
    //-----------------------------------------------------------------------
    /**
     * In the meaning of
     * <ul>
     * <li>For bitbucket it is "groups"</li>
     * <li>For github it is "organizations" in combination with "teams" (we are not going to use this)</li>
     * </ul>.
     *
     * @param organization the organization
     * @return list of groups
     */
    List<Group> getGroupsForOrganization(Organization organization);

    /**
     * Supports invitation.
     *
     * @param organization the organization
     * @return true, if supports invitation for given organization
     */
    boolean supportsInvitation(Organization organization);

    /**
     * Invite user.
     *
     * @param organization the organization
     * @param groupSlugs the group slugs
     * @param userEmail the user email
     */
    void inviteUser(Organization organization, Collection<String> groupSlugs, String userEmail);

    String getBranchUrl(Repository repository, Branch branch);

    String getCreatePullRequestUrl(Repository repository, String sourceSlug, final String sourceBranch, String destinationSlug, final String destinationBranch, String eventSource);

    void startSynchronisation(Repository repo, EnumSet<SynchronizationFlag> flags, int auditId);

    boolean isSyncDisabled(Repository repo, EnumSet<SynchronizationFlag> flags);
}
