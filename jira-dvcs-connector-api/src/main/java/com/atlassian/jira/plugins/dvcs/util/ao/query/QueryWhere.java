package com.atlassian.jira.plugins.dvcs.util.ao.query;

import com.atlassian.jira.plugins.dvcs.util.ao.query.criteria.QueryCriterion;
import com.atlassian.jira.plugins.dvcs.util.ao.query.term.QueryTerm;

/**
 * Where part of query.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class QueryWhere extends DefaultQueryNode implements QueryTerm
{

    /**
     * @see #QueryWhere(QueryCriterion)
     */
    private final QueryCriterion whereCriterion;

    /**
     * Constructor.
     * 
     * @param where
     *            criterion
     */
    public QueryWhere(QueryCriterion where)
    {
        this.whereCriterion = where;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildWhere(QueryContext context, StringBuilder where)
    {
        super.buildWhere(context, where);
        whereCriterion.buildWhere(context, where);
    }

}
