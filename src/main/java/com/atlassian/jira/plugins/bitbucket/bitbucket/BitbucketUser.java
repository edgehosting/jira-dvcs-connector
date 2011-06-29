package com.atlassian.jira.plugins.bitbucket.bitbucket;

/**
 * Describes a Bitbucket user
 */
public interface BitbucketUser
{
    String getUsername();

    String getFirstName();

    String getLastName();

    String getAvatar();

    String getResourceUri();
}
