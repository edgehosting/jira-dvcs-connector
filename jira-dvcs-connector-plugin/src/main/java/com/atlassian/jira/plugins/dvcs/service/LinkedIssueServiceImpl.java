package com.atlassian.jira.plugins.dvcs.service;

import java.util.HashSet;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.sync.impl.IssueKeyExtractor;

/**
 * A {@link LinkedIssueService} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class LinkedIssueServiceImpl implements LinkedIssueService
{

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getIssueKeys(String... source)
    {
        Set<String> result = new HashSet<String>();

        for (String sourceElement : source)
        {
            result.addAll(IssueKeyExtractor.extractIssueKeys(sourceElement));
        }

        return result;
    }

}
