package com.atlassian.jira.plugins.bitbucket.api;

/**
 * TODO come up with better name
 */
public interface ChangesetCache
{
    /**
     * Returns true if the changeset with give nodeId has been already synchronised.
     * 
     * @param repositoryId
     * @param node
     * @return
     */
    boolean isChangesetInDB(int repositoryId, String node);
}
