package com.atlassian.jira.plugins.dvcs.activeobjects;

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
        //TODO: use activeObjects.deleteWithSQL() when AO update https://ecosystem.atlassian.net/browse/AO-348 is available.
        log.debug("Deleting type {}", entityType);
        int remainingEntities = activeObjects.count(entityType, query);
        while (remainingEntities > 0)
        {
            log.debug("Deleting up to {} entities of {} remaining.", DELETE_WINDOW_SIZE, remainingEntities);
            T[] entities = activeObjects.find(entityType, query.limit(DELETE_WINDOW_SIZE));
            activeObjects.delete(entities);
            remainingEntities = activeObjects.count(entityType, query);
        }
    }
}
