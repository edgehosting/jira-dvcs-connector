package com.atlassian.jira.plugins.dvcs.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.java.ao.Entity;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.google.common.base.Joiner;

public class ActiveObjectsUtils
{
    private static final Logger log = LoggerFactory.getLogger(ActiveObjectsUtils.class);
    private static final int DELETE_WINDOW_SIZE = Integer.getInteger("dvcs.connector.delete.window", 500);

    public static <T extends Entity> int delete(final ActiveObjects activeObjects, Class<T> entityType, Query query)
    {
        //TODO: use activeObjects.deleteWithSQL() when AO update https://ecosystem.atlassian.net/browse/AO-348 is available.
        log.debug("Deleting type {}", entityType);
        int deleted = 0;
        int remainingEntities = activeObjects.count(entityType, copyQuery(query, true));
        while (remainingEntities > 0)
        {

            log.debug("Deleting up to {} entities of {} remaining.", DELETE_WINDOW_SIZE, remainingEntities);
            // BBC-453 we need to copy Query as ActiveObjects.find will mangle query for all types annotated by @Preload
            T[] entities = activeObjects.find(entityType, copyQuery(query, false).limit(DELETE_WINDOW_SIZE));
            activeObjects.delete(entities);
            deleted++;
            remainingEntities = activeObjects.count(entityType, copyQuery(query, true));
        }
        return deleted;
    }

    private static Query copyQuery(Query query, boolean forCount)
    {
        Query newQuery;
        Iterable<String> fields = query.getFields();
        if (fields.iterator().hasNext())
        {
            newQuery = Query.select(Joiner.on(",").join(query.getFields()));
        } else
        {
            newQuery = Query.select();
        }
        newQuery.where(query.getWhereClause(), query.getWhereParams())
                .group(query.getGroupClause())
                .offset(query.getOffset());

        if (!forCount)
        {
            newQuery
                .order(query.getOrderClause())
                .limit(query.getLimit());

        }
        if (query.getTable() != null)
        {
            newQuery.from(query.getTable());
        }

        Class<? extends RawEntity<?>> tableType = query.getTableType();
        if (tableType != null)
        {
            newQuery.from(query.getTableType());
            addAlias(newQuery, tableType, query.getAlias(tableType));
        }

        if (query.isDistinct())
        {
            newQuery.distinct();
        }

        Map<Class<? extends RawEntity<?>>, String> joins = query.getJoins();
        for (Entry<Class<? extends RawEntity<?>>, String> joinEntry : joins.entrySet())
        {
            newQuery.join(joinEntry.getKey(), joinEntry.getValue());
            addAlias(newQuery, joinEntry.getKey(), query.getAlias(joinEntry.getKey()));
        }

        return newQuery;
    }

    private static Query addAlias(Query query, Class<? extends RawEntity<?>> table, String alias)
    {
        if (alias != null)
        {
            query.alias(table, alias);
        }
        return query;
    }

    public static String stripToLimit(String s, int limit)
    {
        if (s != null && s.length() > limit)
        {
            s = s.substring(0,limit);
        }

        return s;
    }

    public static StringBuilder renderListStringsOperator(final String column, final String operator, final String joinWithOperator, final Iterable<String> values)
    {
        return renderListOperator(column, operator, joinWithOperator, values);
    }

    public static StringBuilder renderListNumbersOperator(final String column, final String operator, final String joinWithOperator, final Iterable<? extends Number> values)
    {
        return renderListOperator(column, operator, joinWithOperator, values);
    }

    @SuppressWarnings("all")
    private static StringBuilder renderListOperator(final String column, final String operator, final String joinWithOperator, final Iterable values)
    {
        final StringBuilder builder = new StringBuilder(column);
        builder.append(" ").append(operator).append(" (");
        final Iterator<Object> valuesIterator = values.iterator();
        int valuesInQuery = 0;
        boolean overThousandValues = false;
        while(valuesIterator.hasNext())
        {
            final Object value = valuesIterator.next();
            if (value != null && StringUtils.isNotEmpty(value + ""))
            {
                if (valuesInQuery > 0)
                {
                    builder.append(", ");
                }
                addValue(builder, value);
                ++valuesInQuery;
                if (valuesInQuery >= 1000)
                {
                    overThousandValues = true;
                    valuesInQuery = 0;
                    builder.append(") ").append(joinWithOperator).append(" ").append(column).append(" ").append(operator).append(" (");
                }
            }
        }
        builder.append(")");
        return overThousandValues ? builder.insert(0, "(").append(")") : builder;
    }

    protected static StringBuilder addValue(final StringBuilder builder, final Object value)
    {
        if (value instanceof String)
        {
            return builder.append("'").append(value).append("'");
        } else
        {
            return builder.append(value);
        }
    }
}
