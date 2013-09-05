package com.atlassian.jira.plugins.dvcs.activeobjects;

/**
 * Contains helper utilities, useful for query building.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface QueryHelper
{

    /**
     * @param plainTableName
     * @return transforms plain table name into full SQL table name - escaped and extended by schema prefix
     */
    String getSqlTableName(String plainTableName);

    /**
     * @param plainColumnName
     * @return transforms plain column name into full SQL column name - escaped, ...
     */
    String getSqlColumnName(String plainColumnName);

}
