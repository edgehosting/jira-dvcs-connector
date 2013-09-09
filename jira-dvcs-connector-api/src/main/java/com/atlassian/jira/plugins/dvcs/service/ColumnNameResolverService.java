package com.atlassian.jira.plugins.dvcs.service;

/**
 * Helper service, which is able to resolve column name for the provided AO entity bean property.
 * 
 * <pre>
 * {@code
 *      columnNameResolverService.column(columnNameResolverService.desc(EntityType.class).getProperty())
 * }
 * </pre>
 * 
 * resp.
 * 
 * <pre>
 * {@code
 * private final EntityType entityTypeDescription = columnNameResolverService.desc(EntityType.class);
 * ...
 *      columnNameResolverService.column(entityTypeDescription.getProperty());
 * ...
 * }
 * </pre>
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface ColumnNameResolverService
{

    /**
     * @param clazz
     *            type of AO entity
     * @return proxy necessary by {@link #column(Object)} resolving
     */
    <T> T desc(Class<T> clazz);

    /**
     * Resolves column name for the getter called over {@link #desc(Class)} proxy. Getter and column call must be paired and called in the
     * same thread.
     * 
     * @param desc
     *            any value returned by getter call on the {@link #desc(Class)} proxy.
     * @return resolved column name
     */
    <T> String column(Object desc);

}
