package com.atlassian.jira.plugins.dvcs.activeobjects;

import net.java.ao.EntityManager;
import net.java.ao.RawEntity;

import java.sql.SQLException;
import java.util.Map;

public abstract class AOPopulator
{
    protected EntityManager entityManager;

    public AOPopulator(final EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    public <T extends RawEntity<K>, K> T create(Class<T> type, Map<String, Object> params)
    {
        try
        {
            return entityManager.create(type, params);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
