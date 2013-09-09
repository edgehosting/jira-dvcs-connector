package com.atlassian.jira.plugins.dvcs.util.ao.query;

import net.java.ao.Query;
import net.java.ao.RawEntity;

/**
 * Holds context, used during {@link Query} building.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface QueryContext
{

    /**
     * @return Current query.
     */
    Query getQuery();

    /**
     * @param entity
     * @return alias of entity
     */
    String getEntityAlias(Class<? extends RawEntity<?>> entity);

    /**
     * Push parameter name into the context.
     * 
     * @param parameterName
     */
    void pushParameter(String parameterName);

    /**
     * Push parameter name & value into the context.
     * 
     * @param parameterName
     * @param parameterValue
     */
    void pushParameter(String parameterName, Object parameterValue);

}
