package com.atlassian.jira.plugins.dvcs.model;

import java.util.List;

/**
 *  An entity that is mapped to issue keys, useful for grouping on the receiving side
 */
public interface IssueKeyedEntity
{
    List<String> getIssueKeys();
}
