package com.atlassian.jira.plugins.dvcs.util.ao.query.term;

import com.atlassian.jira.plugins.dvcs.util.ao.query.DefaultQueryNode;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryContext;
import net.java.ao.RawEntity;

/**
 * Defines table column - necessary because of entity alias processing.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class QueryColumn extends DefaultQueryNode implements QueryTerm
{

    /**
     * @see #QueryColumn(Class, String)
     */
    private final Class<? extends RawEntity<?>> entity;

    /**
     * @see #QueryColumn(Class, String)
     */
    private final String columnName;

    /**
     * Constructor.
     * 
     * @param entity
     *            name of entity
     * @param columnName
     *            name of column
     */
    public QueryColumn(Class<? extends RawEntity<?>> entity, String columnName)
    {
        this.entity = entity;
        this.columnName = columnName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String joinOn(QueryContext context)
    {
        return context.getEntityAlias(entity) + "." + columnName;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String buildOrder(QueryContext context)
    {
        return context.getEntityAlias(entity) + "." + columnName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildWhere(QueryContext context, StringBuilder where)
    {
        where.append(context.getEntityAlias(entity)).append('.').append(columnName);
    }

}
