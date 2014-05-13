package com.atlassian.jira.plugins.dvcs.activeobjects;

import com.atlassian.activeobjects.spi.DataSourceProvider;
import com.atlassian.activeobjects.spi.DatabaseType;
import com.atlassian.plugin.PluginAccessor;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import javax.annotation.Resource;

/**
 * An implementation of {@link QueryHelper}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class QueryHelperImpl implements QueryHelper
{
    
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryHelperImpl.class);

    /**
     * Injected {@link PluginAccessor} dependency.
     */
    @Resource
    private PluginAccessor pluginAccessor;

    /**
     * Injected {@link DataSourceProvider} dependency.
     */
    @Resource
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
     * Lazy initialization of information provided by connection.
     */
    private synchronized void init()
    {
        if (!initialized)
        {
            Connection connection = null;
            try
            {
                this.schema = dataSourceProvider.getSchema();
                
                connection = dataSourceProvider.getDataSource().getConnection();
                this.quote = connection.getMetaData().getIdentifierQuoteString();
                initialized = true;

            } catch (SQLException e)
            {
                throw new RuntimeException(e);
                
            } finally {
                if (connection != null) {
                    try
                    {
                        connection.close();
                    } catch (SQLException e)
                    {
                        LOGGER.error("Unable to close connection!", e);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAlias(String plainAlias)
    {
        init();
        // TODO: BUG inside AO in case of ORACLE:
        // AO does not quote aliases except of group by clause
        // mixed quoted and unquoted aliases are problems, because oracle does automatically upper case of unquoted aliases
        // this workaround does that all aliases in case of oracle will be upper-cased, regardless if it is quoted or not.
        if (DatabaseType.ORACLE.equals(dataSourceProvider.getDatabaseType())) {
            return plainAlias.toUpperCase();
        } else {
            return plainAlias; 
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOrder(OrderClause... orderClause)
    {

        Function<OrderClause, String> orderClauseToString;

        if (isBugAOSupportOnlySingleColumnInsideOrderClause())
        {
            orderClauseToString = new Function<OrderClause, String>()
            {

                private boolean isFirst = true;

                @Override
                public String apply(OrderClause input)
                {
                    if (isFirst)
                    {
                        isFirst = false;
                        return input.getColumn() + ' ' + input.getOrder().name();
                    } else
                    {
                        return getSqlColumnName(input.getColumn()) + ' ' + input.getOrder().name();
                    }
                }

            };
        } else
        {
            orderClauseToString = new Function<OrderClause, String>()
            {

                @Override
                public String apply(OrderClause input)
                {
                    return input.getColumn() + ' ' + input.getOrder().name();
                }

            };
        }

        return Joiner.on(", ").join(Iterables.transform(Arrays.asList(orderClause), orderClauseToString));
    }

    /**
     * @return True if AO support only single column inside order clause.
     */
    private boolean isBugAOSupportOnlySingleColumnInsideOrderClause()
    {
        return isBeforeAOVersion("0.20");
    }

    /**
     * Constrain for AO version - current AO version must be less than provided version.
     * 
     * @param version
     *            expected version constrain
     * @return true if current version is less than provided
     */
    private boolean isBeforeAOVersion(String version)
    {
        String aoVersion = pluginAccessor.getPlugin("com.atlassian.activeobjects.activeobjects-plugin").getPluginInformation().getVersion();

        String[] aoVersionComponents = aoVersion.split("\\.");
        String[] versionCompoennts = version.split("\\.");

        for (int i = 0; i < versionCompoennts.length; i++)
        {
            if (aoVersionComponents.length < i)
            {
                break;
            }

            try
            {
                boolean isLast = versionCompoennts.length == i;
                int aoVersionComponent = Integer.parseInt(aoVersionComponents[i]);
                int versionComponent = Integer.parseInt(versionCompoennts[i]);

                if (aoVersionComponent > versionComponent || (isLast && aoVersionComponent == versionComponent))
                {
                    return false;
                }
            } catch (NumberFormatException e)
            {
                return false;
            }
        }

        return true;
    }

}
