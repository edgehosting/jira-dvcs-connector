package com.atlassian.jira.plugins.dvcs.dao;

import java.util.Set;

/**
 * A Function that can be called with a batch of Issue Keys, typically whilst processing a collection of Issue Key to
 * Entity mappings
 */
public interface IssueToMappingFunction
{
    /**
     * Execute the function.
     *
     * @return true if processing should continue, false if the processing should stop
     */
    boolean execute(String dvcsType, int repositoryId, Set<String> issueKeys);
}
