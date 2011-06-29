package com.atlassian.jira.plugins.bitbucket.mapper;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketAuthentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangeset;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketRepository;

import java.util.List;

/**
 * Maps bitbucket repositories and commits to jira projects and issues.
 */
public interface BitbucketMapper
{
    /**
     * Return a list of all repositories mapped to the given jira project.
     * @param projectKey the jira project
     * @return a list of repositories
     */
    List<BitbucketRepository> getRepositories(String projectKey);

    /**
     * Map a repository to the specified jira project
     * @param projectKey the jira project
     * @param repository the bitbucket repository
     * @param username the username to use to connect to this bitbucket repository
     * @param password the password to use to connect to this bitbucket repository
     */
    void addRepository(String projectKey, BitbucketRepository repository, String username, String password);

    /**
     * Remove the mapping of the bibucket repository from the specified jira project
     * @param projectKey the jira project
     * @param owner the owner of the repository to remove
     * @param slug the slug of the repository to remove
     */
    void removeRepository(String projectKey, String owner, String slug);

    /**
     * Return a list of all commits mapped to the given issue from the given repository
     * @param issueId the jira issue id
     * @return a list of changesets
     */
    List<BitbucketChangeset> getChangesets(String issueId);

    /**
     * Map a changeset to an issue id for the given repository
     * @param issueId the jira issue id
     * @param bitbucketChangeset the changeset to map to
     */
    void addChangeset(String issueId, BitbucketChangeset bitbucketChangeset);

    /**
     * Remove the mapping of the bibucket changeset from the specified jira issue
     * @param issueId the jira issueid
     * @param bitbucketChangeset the changeset to remove from the mapping
     */
    void removeChangeset(String issueId, BitbucketChangeset bitbucketChangeset);

    /**
     * Load the repository authentication details stored for the mapped repository
     * @param projectKey the project key this repository is mapped to
     * @param owner the owner of the bitbucket repository
     * @param slug the slug of the bitbucket repository
     * @return the authentication details
     */
    BitbucketAuthentication getAuthentication(String projectKey, String owner, String slug);

}
