package com.atlassian.jira.plugins.dvcs.util.ao.query.term;

import com.atlassian.jira.plugins.dvcs.util.ao.query.DefaultQueryNode;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryContext;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryNode;

/**
 * {@link QueryNode} implementation over aliased/named parameter
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class QueryParameter extends DefaultQueryNode implements QueryTerm
{

    /**
     * @see #QueryParameter(String, Object)
     */
    private final String parameterName;

    /**
     * @see #QueryParameter(String, Object)
     */
    private final Object parameterValue;

    /**
     * Constructor.
     * 
     * @param parameterName
     *            name/alias of parameter
     */
    public QueryParameter(String parameterName)
    {
        this(parameterName, null);
    }

    /**
     * Constructor.
     * 
     * @param parameterName
     *            name/alias of parameter
     * @param parameterValue
     *            value of parameter
     */
    public QueryParameter(String parameterName, Object parameterValue)
    {
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
    }

    @Override
    public void buildWhere(QueryContext context, StringBuilder where)
    {
        where.append('?');
        if (parameterValue != null)
        {
            context.pushParameter(parameterName, parameterValue);

        } else
        {
            context.pushParameter(parameterName);

        }
    }

}
