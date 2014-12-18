package com.atlassian.jira.plugins.dvcs.activeobjects;

import com.atlassian.pocketknife.spi.querydsl.AbstractConnectionProvider;
import net.java.ao.EntityManager;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection provider that wraps around an AO entity manager and returns connections from its provider
 */
public class TestConnectionProvider extends AbstractConnectionProvider
{
    private final EntityManager entityManager;

    public TestConnectionProvider(final EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    @Override
    protected Connection getConnectionImpl(final boolean autoCommit)
    {
        try
        {
            return entityManager.getProvider().getConnection();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
