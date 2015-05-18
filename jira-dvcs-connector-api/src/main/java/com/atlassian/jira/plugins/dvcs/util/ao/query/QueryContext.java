package com.atlassian.jira.plugins.dvcs.util.ao.query;

import com.atlassian.jira.plugins.dvcs.activeobjects.QueryHelper;
import net.java.ao.Query;
import net.java.ao.RawEntity;

import java.util.Map;

/**
 * Holds context, used during {@link Query} building.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface QueryContext
{

    /**
     * @return Provides access to {@link QueryHelper} service.
     */
    QueryHelper getQueryHelper();

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
     * Push parameter name and value into the context.
     * 
     * @param parameterName
     * @param parameterValue
     */
    void pushParameter(String parameterName, Object parameterValue);

    /**
     * @return Returns provided parameters - which are used for current {@link Query} building.
     */
    Map<String, Object> getProvidedParameters();

}
