package com.atlassian.jira.plugins.dvcs.util.ao.query;

/**
 * Empty/default implementation of {@link QueryNode} methods.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class DefaultQueryNode implements QueryNode
{

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildJoin(QueryContext context)
    {
        // override if necessary
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String joinOn(QueryContext context)
    {
        // override if necessary
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildWhere(QueryContext context, StringBuilder where)
    {
        // override if necessary
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String buildOrder(QueryContext context)
    {
        return "";
    }

}
