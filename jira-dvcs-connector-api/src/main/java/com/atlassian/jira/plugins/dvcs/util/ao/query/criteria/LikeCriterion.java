package com.atlassian.jira.plugins.dvcs.util.ao.query.criteria;

import com.atlassian.jira.plugins.dvcs.util.ao.query.DefaultQueryNode;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryContext;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryNode;
import com.atlassian.jira.plugins.dvcs.util.ao.query.term.QueryColumn;
import com.atlassian.jira.plugins.dvcs.util.ao.query.term.QueryParameter;
import com.atlassian.jira.plugins.dvcs.util.ao.query.term.QueryTerm;

/**
 * {@link QueryNode} implementation of {@link LikeCriterion}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class LikeCriterion extends DefaultQueryNode implements QueryCriterion
{

    /**
     * @see #LikeCriteria(QueryColumn, QueryParameter)
     */
    private final QueryTerm column;

    /**
     * @see #LikeCriteria(QueryColumn, QueryParameter)
     */
    private final QueryTerm parameter;

    /**
     * Constructor.
     * 
     * @param column
     *            over which column
     * @param parameter
     *            like parameter
     */
    public LikeCriterion(QueryColumn column, QueryParameter parameter)
    {
        this.column = column;
        this.parameter = parameter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildWhere(QueryContext context, StringBuilder where)
    {
        column.buildWhere(context, where);
        where.append(" like ");
        parameter.buildWhere(context, where);
    }

}
