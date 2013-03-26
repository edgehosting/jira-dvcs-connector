package com.atlassian.jira.plugins.dvcs.util;

import java.util.HashSet;
import java.util.Set;

import java.util.Map;
import java.util.Map.Entry;

import net.java.ao.Entity;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.google.common.base.Joiner;

public class ActiveObjectsUtils
{
    private static final Logger log = LoggerFactory.getLogger(ActiveObjectsUtils.class);
    private static final int DELETE_WINDOW_SIZE = Integer.getInteger("dvcs.connector.delete.window", 500); 

    public static <T extends Entity> void delete(final ActiveObjects activeObjects, Class<T> entityType, Query query)
    {
        delete(activeObjects, entityType, query, false);
    }
    
    public static <T extends Entity> Set<Integer> deleteAndReturnIds(final ActiveObjects activeObjects, Class<T> entityType, Query query)
    {
        return delete(activeObjects, entityType, query, true);
    }
    
    private static <T extends Entity> Set<Integer> delete(final ActiveObjects activeObjects, final Class<T> entityType, final Query query, final boolean returnIds)
    {
        //TODO: use activeObjects.deleteWithSQL() when AO update https://ecosystem.atlassian.net/browse/AO-348 is available.
        log.debug("Deleting type {}", entityType);
        query.setTableType(entityType);
        int remainingEntities = activeObjects.count(entityType, query);
        
        Set<Integer> deletedIds = null;
        if (returnIds)
        {
            deletedIds = new HashSet<Integer>();
        }
        while (remainingEntities > 0)
        {
            
            log.debug("Deleting up to {} entities of {} remaining.", DELETE_WINDOW_SIZE, remainingEntities);
            // BBC-453 we need to copy Query as ActiveObjects.find will mangle query for all types annotated by @Preload 
            T[] entities = activeObjects.find(entityType, copyQuery(query).limit(DELETE_WINDOW_SIZE));
            if ( returnIds )
            {
                for ( T entity : entities)
                {
                    deletedIds.add(entity.getID());
                }
            }
            activeObjects.delete(entities);
            remainingEntities = activeObjects.count(entityType, query);
        }
        
        return deletedIds;
    }
    
    public static Query copyQuery(Query query)
    {
        Query newQuery = Query.select(Joiner.on(",").join(query.getFields()))
                .where(query.getWhereClause(), query.getWhereParams())
                .order(query.getOrderClause())
                .group(query.getGroupClause())
                .offset(query.getOffset())
                .limit(query.getLimit());
     
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
            query.join(joinEntry.getKey(), joinEntry.getValue());
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
}
