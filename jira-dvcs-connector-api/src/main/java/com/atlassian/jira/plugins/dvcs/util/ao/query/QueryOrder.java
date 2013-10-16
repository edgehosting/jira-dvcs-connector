package com.atlassian.jira.plugins.dvcs.util.ao.query;

import com.atlassian.jira.plugins.dvcs.util.ao.query.term.QueryColumn;

/**
 * Order part of clause.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class QueryOrder extends DefaultQueryNode
{

    /**
     * @see #QueryOrder(QueryColumn, boolean)
     */
    private QueryColumn column;

    /**
     * @see #QueryOrder(QueryColumn, boolean)
     */
    private boolean asc;

    /**
     * Constructor.
     * 
     * @param column
     * @param asc
     */
    public QueryOrder(QueryColumn column, boolean asc)
    {
        this.column = column;
        this.asc = asc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String buildOrder(QueryContext context)
    {
        return column.buildOrder(context) + (asc ? " ASC " : " DESC ");
    }

}
