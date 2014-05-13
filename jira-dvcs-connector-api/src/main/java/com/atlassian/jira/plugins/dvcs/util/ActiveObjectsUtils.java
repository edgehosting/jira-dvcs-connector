package com.atlassian.jira.plugins.dvcs.util;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.google.common.collect.Iterables;
import net.java.ao.Entity;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ActiveObjectsUtils
{
    private static final Logger log = LoggerFactory.getLogger(ActiveObjectsUtils.class);
    private static final int DELETE_WINDOW_SIZE = Integer.getInteger("dvcs.connector.delete.window", 500);
    // Because of an issue in ActiveObjects (AO-453, AO-455) we can't use deleteWithSQL in PostgresSQL
    private static final boolean DELETE_WITH_SQL = false; //SystemUtils.getMethodExists(ActiveObjects.class, "deleteWithSQL", Class.class, String.class, Object[].class);

    public static <T extends Entity> int delete(final ActiveObjects activeObjects, Class<T> entityType, Query query)
    {
        log.debug("Deleting type {}", entityType);

        final Set<Integer> ids = new HashSet<Integer>();
        activeObjects.stream(entityType, query, new EntityStreamCallback<T, Integer>()
        {

            @Override
            public void onRowRead(T t)
            {
                ids.add(t.getID());
            }

        });

        int deleted = 0;
        Iterable<List<Integer>> windows = Iterables.partition(ids, DELETE_WINDOW_SIZE);
        for (List<Integer> window : windows)
        {
            log.debug("Deleting up to {} entities of {} remaining.", DELETE_WINDOW_SIZE, ids.size() - deleted);
            if (DELETE_WITH_SQL)
            {
                activeObjects.deleteWithSQL(entityType, renderListNumbersOperator("ID", "IN", "OR", window).toString());
            } else
            {
                activeObjects.delete(activeObjects.find(entityType, renderListNumbersOperator("ID", "IN", "OR", window).toString()));
            }
            deleted += window.size();
        }

        return deleted;
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
