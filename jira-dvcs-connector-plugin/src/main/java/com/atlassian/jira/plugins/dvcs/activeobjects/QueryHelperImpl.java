package com.atlassian.jira.plugins.dvcs.activeobjects;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;

import com.atlassian.activeobjects.spi.DataSourceProvider;

/**
 * An implementation of {@link QueryHelper}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class QueryHelperImpl implements QueryHelper
{

    /**
     * @see #QueryHelperImpl(DataSourceProvider)
     */
    private DataSourceProvider dataSourceProvider;

    /**
     * @see DataSourceProvider#getSchema()
     */
    private String schema;

    /**
     * @see DatabaseMetaData#getIdentifierQuoteString()
     */
    private String quote;

    /**
     * @see #init()
     */
    private boolean initialized;

    /**
     * Constructor.
     * 
     * @param dataSourceProvider
     *            injected {@link DataSourceProvider} dependency
     */
    public QueryHelperImpl(DataSourceProvider dataSourceProvider)
    {
        this.dataSourceProvider = dataSourceProvider;
    }

    /**
     * Lazy initialization of information provided by connection.
     */
    private synchronized void init()
    {
        if (!initialized)
        {
            try
            {
                this.schema = dataSourceProvider.getSchema();
                this.quote = dataSourceProvider.getDataSource().getConnection().getMetaData().getIdentifierQuoteString();
                initialized = true;

            } catch (SQLException e)
            {
                throw new RuntimeException(e);

            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSqlTableName(String plainTableName)
    {
        init();

        String result = "";

        // add schema if necessary
        if (StringUtils.isNotBlank(schema))
        {
            result += schema + ".";
        }

        // quotes if necessary
        if (StringUtils.isNotBlank(quote))
        {
            result += quote + plainTableName + quote;
        } else
        {
            result += plainTableName;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSqlColumnName(String plainColumnName)
    {
        init();

        // quotes if necessary
        if (StringUtils.isNotBlank(quote))
        {
            return quote + plainColumnName + quote;
        } else
        {
            return plainColumnName;
        }

    }

}
