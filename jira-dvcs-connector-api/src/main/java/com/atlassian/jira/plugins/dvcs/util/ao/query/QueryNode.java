package com.atlassian.jira.plugins.dvcs.util.ao.query;

import net.java.ao.Query;

/**
 * Contract for query form implementations.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface QueryNode
{

    /**
     * Builds join part.
     * 
     * @param context
     *            for building
     * @see Query#join(Class)
     */
    void buildJoin(QueryContext context);

    /**
     * 
     * @param context
     * @return
     */
    String joinOn(QueryContext context);

    /**
     * Builds where part.
     * 
     * @param context
     *            for building
     * @param where
     *            part
     */
    void buildWhere(QueryContext context, StringBuilder where);

}
