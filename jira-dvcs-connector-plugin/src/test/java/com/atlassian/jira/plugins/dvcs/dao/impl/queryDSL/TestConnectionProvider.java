package com.atlassian.jira.plugins.dvcs.dao.impl.queryDSL;

import com.atlassian.pocketknife.spi.querydsl.AbstractConnectionProvider;
import net.java.ao.EntityManager;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * TODO: Document this class / interface here
 *
 * @since v6.3
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
