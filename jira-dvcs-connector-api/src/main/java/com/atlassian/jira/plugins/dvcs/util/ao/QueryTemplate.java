package com.atlassian.jira.plugins.dvcs.util.ao;

import com.atlassian.jira.plugins.dvcs.activeobjects.QueryHelper;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryContext;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryJoin;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryOrder;
import com.atlassian.jira.plugins.dvcs.util.ao.query.QueryWhere;
import com.atlassian.jira.plugins.dvcs.util.ao.query.criteria.AndCriterion;
import com.atlassian.jira.plugins.dvcs.util.ao.query.criteria.EqCriterion;
import com.atlassian.jira.plugins.dvcs.util.ao.query.criteria.InCriterion;
import com.atlassian.jira.plugins.dvcs.util.ao.query.criteria.LikeCriterion;
import com.atlassian.jira.plugins.dvcs.util.ao.query.criteria.OrCriterion;
import com.atlassian.jira.plugins.dvcs.util.ao.query.criteria.QueryCriterion;
import com.atlassian.jira.plugins.dvcs.util.ao.query.term.QueryColumn;
import com.atlassian.jira.plugins.dvcs.util.ao.query.term.QueryParameter;
import com.atlassian.jira.plugins.dvcs.util.ao.query.term.QueryTerm;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Template for queries.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public abstract class QueryTemplate
{

    /**
     * @see #QueryTemplate(QueryHelper)
     */
    private final QueryHelper queryHelper;

    /**
     * Entity aliases.
     */
    private final Map<Class<? extends RawEntity<?>>, String> aliases = new ConcurrentHashMap<Class<? extends RawEntity<?>>, String>();

    /**
     * Join parts.
     */
    private final List<QueryJoin> joins = new LinkedList<QueryJoin>();

    /**
     * Where clause.
     */
    private QueryWhere where;

    /**
     * Order clause.
     */
    private QueryOrder[] orders = new QueryOrder[] {};

    /**
     * Constructor.
     */
    public QueryTemplate(QueryHelper queryHelper)
    {
        this.queryHelper = queryHelper;
        build();
    }

    /**
     * @return Responsible for query building.
     */
    protected abstract void build();

    /**
     * Registers alias for provided entity.
     * 
     * @param entity
     *            type of entity
     * @param alias
     *            appropriate alias
     */
    protected void alias(Class<? extends RawEntity<?>> entity, String alias)
    {
        aliases.put(entity, alias);
    }

    /**
     * Joins provided entity.
     * 
     * @param entity
     *            to join
     * @param leftColumn
     *            on which column
     * @param rightColumn
     *            by which column
     */
    protected void join(Class<? extends RawEntity<?>> entity, QueryColumn leftColumn, String rightColumn)
    {
        joins.add(new QueryJoin(entity, leftColumn, column(entity, rightColumn)));
    }

    /**
     * Where clause.
     * 
     * @param where
     *            clause
     */
    protected void where(QueryCriterion where)
    {
        this.where = new QueryWhere(where);
    }

    /**
     * Order clause.
     * 
     * @param orders
     *            parts
     */
    protected void order(QueryOrder... orders)
    {
        this.orders = orders;
    }

    /**
     * @param orderBy
     * @param asc
     * @return {@link QueryOrder}
     */
    protected QueryOrder orderBy(QueryColumn orderBy, boolean asc)
    {
        return new QueryOrder(orderBy, asc);
    }

    /**
     * @param criteria
     * @return {@link AndCriterion}
     */
    protected QueryCriterion and(QueryCriterion... criteria)
    {
        if (criteria.length == 1)
        {
            return criteria[0];

        } else
        {
            return new AndCriterion(criteria);

        }
    }

    /**
     * @param criteria
     * @return {@link OrCriterion}
     */
    protected QueryCriterion or(QueryCriterion... criteria)
    {
        if (criteria.length == 1)
        {
            return criteria[0];

        } else
        {
            return new OrCriterion(criteria);

        }
    }

    /**
     * @param leftSide
     * @param rightSide
     * @return {@link EqCriterion}
     */
    protected QueryCriterion eq(QueryTerm leftSide, QueryTerm rightSide)
    {
        return new EqCriterion(leftSide, rightSide);
    }

    /**
     * @param column
     * @param in
     * @return {@link InCriterion}
     */
    protected QueryCriterion in(QueryColumn column, QueryParameter in)
    {
        return new InCriterion(column, in);
    }

    /**
     * @param leftSide
     * @param rightSide
     * @return {@link EqCriterion}-s
     */
    protected QueryCriterion[] eq(QueryTerm leftSide, QueryTerm... rightSide)
    {
        QueryCriterion[] result = new QueryCriterion[rightSide.length];
        for (int i = 0; i < rightSide.length; i++)
        {
            result[i] = eq(leftSide, rightSide[i]);
        }

        return result;
    }

    /**
     * @param column
     * @param parameter
     * @return {@link LikeCriterion}
     */
    protected QueryCriterion like(QueryColumn column, QueryParameter parameter)
    {
        return new LikeCriterion(column, parameter);
    }

    /**
     * @param column
     * @param parameters
     * @return {@link LikeCriterion}
     */
    protected QueryCriterion[] like(QueryColumn column, QueryParameter... parameters)
    {
        QueryCriterion[] result = new QueryCriterion[parameters.length];

        for (int i = 0; i < parameters.length; i++)
        {
            result[i] = new LikeCriterion(column, parameters[i]);
        }

        return result;
    }

    /**
     * @param entity
     * @param columnName
     * @return {@link QueryColumn}
     */
    protected QueryColumn column(Class<? extends RawEntity<?>> entity, String columnName)
    {
        return new QueryColumn(entity, columnName);
    }

    /**
     * @param parameterName
     * @return {@link QueryParameter}
     */
    protected QueryParameter parameter(String parameterName)
    {
        return new QueryParameter(parameterName);
    }

    /**
     * @param parameterName
     * @param parameterValue
     * @return {@link QueryParameter}
     */
    protected QueryParameter parameter(String parameterName, Object parameterValue)
    {
        return new QueryParameter(parameterName, parameterValue);
    }

    /**
     * @param parameterName
     * @param parameterValue
     * @return {@link QueryParameter}-s
     */
    protected QueryParameter[] parameter(String parameterName, Object... parameterValue)
    {
        QueryParameter[] result = new QueryParameter[parameterValue.length];
        for (int i = 0; i < parameterValue.length; i++)
        {
            result[i] = new QueryParameter(parameterName + "-" + i, parameterValue[i]);
        }

        return result;
    }

    /**
     * Builds/transform to AO query.
     * 
     * @param parameters
     * @return
     */
    public Query toQuery(final Map<String, Object> parameters)
    {
        final List<String> namedParameters = new LinkedList<String>();

        final Map<String, Object> initialParameterValues = new HashMap<String, Object>();

        final Query result = Query.select();

        QueryContext context = new QueryContext()
        {

            @Override
            public QueryHelper getQueryHelper()
            {
                return queryHelper;
            }

            @Override
            public void pushParameter(String parameterName)
            {
                namedParameters.add(parameterName);
            }

            @Override
            public void pushParameter(String parameterName, Object parameterValue)
            {
                namedParameters.add(parameterName);
                initialParameterValues.put(parameterName, parameterValue);
            }

            @Override
            public String getEntityAlias(Class<? extends RawEntity<?>> entity)
            {
                String result = aliases.get(entity);
                if (StringUtils.isEmpty(result))
                {
                    throw new RuntimeException("Each entity must be aliased. There was no alias for provided entity: " + entity);
                }

                return queryHelper.getAlias(result);
            }

            @Override
            public Query getQuery()
            {
                return result;
            }

            @Override
            public Map<String, Object> getProvidedParameters()
            {
                return parameters;
            }

        };

        buildAliases(context);
        buildJoins(context);

        Map<String, Object> mergedParameters = new HashMap<String, Object>(initialParameterValues);
        mergedParameters.putAll(parameters);
        buildWhere(context, namedParameters, mergedParameters);

        buildOrder(context);

        return result;
    }

    /**
     * Builds aliases.
     * 
     * @param context
     *            query context
     */
    private void buildAliases(QueryContext context)
    {
        for (Entry<Class<? extends RawEntity<?>>, String> alias : aliases.entrySet())
        {
            context.getQuery().alias(alias.getKey(), alias.getValue());
        }
    }

    /**
     * Builds joins of query.
     * 
     * @param context
     *            query context
     */
    private void buildJoins(QueryContext context)
    {
        for (QueryJoin join : joins)
        {
            join.buildJoin(context);
        }
    }

    /**
     * Builds where part of query.
     * 
     * @param context
     *            query context
     * @param namedParameters
     * @param parameters
     */
    private void buildWhere(QueryContext context, List<String> namedParameters, Map<String, Object> parameters)
    {
        StringBuilder result = new StringBuilder();
        where.buildWhere(context, result);
        Object[] queryParameters = toQueryParameters(parameters, namedParameters);
        if (result.length() > 0)
        {
            context.getQuery().where(result.toString(), queryParameters);
        }
    }

    /**
     * Builds order part of query.
     * 
     * @param context
     *            query context
     */
    private void buildOrder(QueryContext context)
    {
        StringBuilder order = new StringBuilder();
        for (int i = 0; i < orders.length; i++)
        {
            order.append(orders[i].buildOrder(context));
            if (i != orders.length - 1)
            {
                order.append(", ");
            }
        }
        String orderClause = order.toString().trim();
        if (!orderClause.isEmpty())
        {
            context.getQuery().order(orderClause);
        }
    }

    /**
     * 
     * @param nameToParameter
     *            parameter aliases
     * @param namedParameters
     *            ordered collection of parameter names
     * @return resolves parameters array of provided parameter names
     */
    private Object[] toQueryParameters(Map<String, Object> nameToParameter, final List<String> namedParameters)
    {
        List<Object> queryParameters = new LinkedList<Object>();
        for (String parameterName : namedParameters)
        {
            if (nameToParameter.containsKey(parameterName))
            {
                Object parameterByName = nameToParameter.get(parameterName);
                if (parameterByName != null && parameterByName.getClass().isArray())
                {
                    int length = Array.getLength(parameterByName);
                    for (int i = 0; i < length; i++)
                    {
                        queryParameters.add(Array.get(parameterByName, i));
                    }

                } else if (parameterByName instanceof Collection<?>)
                {
                    queryParameters.addAll((Collection<?>) parameterByName);

                } else
                {
                    queryParameters.add(parameterByName);

                }

            } else
            {
                throw new RuntimeException("Unable to find named parameter: " + parameterName);

            }
        }
        return queryParameters.toArray();
    }

}
