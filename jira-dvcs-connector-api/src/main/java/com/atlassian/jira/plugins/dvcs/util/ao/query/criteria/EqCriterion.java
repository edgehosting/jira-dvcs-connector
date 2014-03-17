package com.atlassian.jira.plugins.dvcs.util.ao.query.criteria;

import com.atlassian.jira.plugins.dvcs.util.ao.query.DefaultQueryNode;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryContext;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryNode;
import com.atlassian.jira.plugins.dvcs.util.ao.query.term.QueryTerm;

/**
 * {@link QueryNode} implementation over '=' condition.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class EqCriterion extends DefaultQueryNode implements QueryCriterion
{

    /**
     * @see #EQCriteria(QueryNode, QueryNode)
     */
    private final QueryTerm leftSide;

    /**
     * @see #EQCriteria(QueryNode, QueryNode)
     */
    private final QueryTerm rightSide;

    /**
     * Constructor.
     * 
     * @param leftSide
     *            left side of equals condition
     * @param rightSide
     *            right side of equals condition
     */
    public EqCriterion(QueryTerm leftSide, QueryTerm rightSide)
    {
        this.leftSide = leftSide;
        this.rightSide = rightSide;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildWhere(QueryContext context, StringBuilder where)
    {
        leftSide.buildWhere(context, where);
        where.append(" = ");
        rightSide.buildWhere(context, where);
    }

}
