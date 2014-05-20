package com.atlassian.jira.plugins.dvcs.util.ao.query;

import com.atlassian.jira.plugins.dvcs.util.ao.query.term.QueryColumn;
import com.atlassian.jira.plugins.dvcs.util.ao.query.term.QueryTerm;
import net.java.ao.RawEntity;

/**
 * {@link QueryTerm} implementation of join clause.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class QueryJoin extends DefaultQueryNode implements QueryTerm
{

    /**
     * @see #QueryJoin(Class, QueryColumn, QueryColumn)
     */
    private final Class<? extends RawEntity<?>> entity;

    /**
     * @see #QueryJoin(Class, QueryColumn, QueryColumn)
     */
    private final QueryColumn leftColumn;

    /**
     * @see #QueryJoin(Class, QueryColumn, QueryColumn)
     */
    private final QueryColumn rightColumn;

    /**
     * Constructor.
     * 
     * @param entity
     *            joined entity
     * @param leftColumn
     *            left column of join
     * @param rightColumn
     *            right column of join
     */
    public QueryJoin(Class<? extends RawEntity<?>> entity, QueryColumn leftColumn, QueryColumn rightColumn)
    {
        this.entity = entity;
        this.leftColumn = leftColumn;
        this.rightColumn = rightColumn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildJoin(QueryContext context)
    {
        super.buildJoin(context);
        context.getQuery().join(entity, leftColumn.joinOn(context) + " = " + rightColumn.joinOn(context));
    }

}
