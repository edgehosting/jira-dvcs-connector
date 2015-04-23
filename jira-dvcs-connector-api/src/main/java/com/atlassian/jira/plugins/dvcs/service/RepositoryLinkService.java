package com.atlassian.jira.plugins.dvcs.service;

import java.util.Set;

/**
 * TODO: Document this class / interface here
 *
 * @since v6.3
 */
public interface RepositoryLinkService
{
    Set<String> getPreviouslyLinkedProjects ();

    void setPreviouslyLinkedProjects(Set<String> projectKeys);

}
