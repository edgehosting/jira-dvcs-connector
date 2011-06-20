package com.atlassian.jira.plugins.bitbucket.bitbucket.connection;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketAuthentication;

/**
 * Interface to the bitbucket service
 */
public interface BitbucketConnection
{
    String getRepository(BitbucketAuthentication auth, String owner, String slug);
    String getChangeset(BitbucketAuthentication auth, String owner, String slug, String id);
    String getChangesets(BitbucketAuthentication auth, String owner, String slug, String start, int limit);
}
