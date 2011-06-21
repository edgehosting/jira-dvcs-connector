package com.atlassian.jira.plugins.bitbucket.repository;

/**
 * Persisted details about a bitbucket repository
 */
public interface Repository
{
    String getProjectKey();
    String getOwner();
    String getSlug();
    String getUrl();
}
