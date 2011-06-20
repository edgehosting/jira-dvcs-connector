package com.atlassian.jira.plugins.bitbucket.bitbucket;

/**
 */
public interface BitbucketUser
{
    String getUsername();
    String getFirstName();
    String getLastName();
    String getAvatar();
    String getResourceUri();
}
