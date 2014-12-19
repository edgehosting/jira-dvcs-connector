package com.atlassian.jira.plugins.dvcs.model;

import java.util.List;
import javax.annotation.Nullable;

/**
 *  An entity that is mapped to issue keys, useful for grouping on the receiving side
 */
public interface IssueKeyedEntity
{
    /**
     * Get the issue keys that are associated with this entity
     * @return The issue keys that are associated with this entity
     */
    @Nullable
    List<String> getIssueKeys();
}
