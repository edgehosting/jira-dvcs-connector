package com.atlassian.jira.plugins.dvcs.util;

import java.util.HashSet;
import java.util.Set;

import net.java.ao.Entity;
import net.java.ao.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;

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
            T[] entities = activeObjects.find(entityType, query.limit(DELETE_WINDOW_SIZE));
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
}
