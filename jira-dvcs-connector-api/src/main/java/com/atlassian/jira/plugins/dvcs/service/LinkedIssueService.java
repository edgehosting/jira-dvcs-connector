package com.atlassian.jira.plugins.dvcs.service;

import java.util.Set;

/**
 * @see #getIssueKeys(String...)
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface LinkedIssueService
{

    /**
     * Provides way how to extract issue keys from provided source - {@link String}'s.
     * 
     * @param source
     *            for extraction
     * @return extracted issues
     */
    Set<String> getIssueKeys(String... source);

}
