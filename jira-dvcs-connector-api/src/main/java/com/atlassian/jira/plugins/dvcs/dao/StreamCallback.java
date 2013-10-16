package com.atlassian.jira.plugins.dvcs.dao;

/**
 * Callbacks for workings in streams.
 * 
 * @author Stanislav Dvorscak
 * 
 * @param <T>
 */
public interface StreamCallback<T>
{

    /**
     * @param e
     */
    void callback(T e);

}
