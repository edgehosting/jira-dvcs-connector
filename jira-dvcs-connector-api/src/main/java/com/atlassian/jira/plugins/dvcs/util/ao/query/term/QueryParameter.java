package com.atlassian.jira.plugins.dvcs.util.ao.query.term;

import com.atlassian.jira.plugins.dvcs.util.ao.query.DefaultQueryNode;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryContext;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryNode;

import java.lang.reflect.Array;
import java.util.Collection;

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
     * True if the initial value was provided.
     */
    private boolean withInitialValue;

    /**
     * Constructor.
     * 
     * @param parameterName
     *            name/alias of parameter
     */
    public QueryParameter(String parameterName)
    {
        this.parameterName = parameterName;
        this.parameterValue = null;
        this.withInitialValue = false;
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
        this.withInitialValue = true;
    }

    @Override
    public void buildWhere(QueryContext context, StringBuilder where)
    {
        Object resolvedValue = withInitialValue ? parameterValue : context.getProvidedParameters().get(parameterName);

        int length;
        if (resolvedValue != null && resolvedValue.getClass().isArray())
        {
            length = Array.getLength(resolvedValue);
        } else if (resolvedValue instanceof Collection<?>)
        {
            length = ((Collection<?>) resolvedValue).size();
        } else
        {
            length = 1;
        }

        for (int i = 0; i < length; i++)
        {
            where.append('?');
            if (i != length - 1)
            {
                where.append(", ");
            }
        }

        if (withInitialValue)
        {
            context.pushParameter(parameterName, parameterValue);

        } else
        {
            context.pushParameter(parameterName);

        }
    }
}
