package com.atlassian.jira.plugins.dvcs.util.ao.query.criteria;

import com.atlassian.jira.plugins.dvcs.util.ao.query.DefaultQueryNode;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryContext;
import com.atlassian.jira.plugins.dvcs.util.ao.query.term.QueryColumn;
import com.atlassian.jira.plugins.dvcs.util.ao.query.term.QueryParameter;

/**
 * IN SQL where clause.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class InCriterion extends DefaultQueryNode implements QueryCriterion
{

    /**
     * @see #InCriterion(QueryColumn, QueryParameter)
     */
    private QueryColumn column;

    /**
     * @see #InCriterion(QueryColumn, QueryParameter)
     */
    private QueryParameter in;

    /**
     * Constructor.
     * 
     * @param column
     *            over which column
     * @param in
     *            terms
     */
    public InCriterion(QueryColumn column, QueryParameter in)
    {
        this.column = column;
        this.in = in;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildWhere(QueryContext context, StringBuilder where)
    {
        super.buildWhere(context, where);
        column.buildWhere(context, where);
        where.append(" in ( ");
        in.buildWhere(context, where);
        where.append(" ) ");
    }

}
