package com.atlassian.jira.plugins.dvcs.dao.impl;

public interface DAOConstants
{
    static final int MAXIMUM_ENTITIES_PER_ISSUE_KEY = Integer.getInteger("dvcs.connector.max.entities.per.key", 401);
}
