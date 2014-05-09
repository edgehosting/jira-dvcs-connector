package com.atlassian.jira.plugins.dvcs.sync.impl;

import com.atlassian.jira.plugins.dvcs.sync.ExecutorServiceFactoryBean;
import com.atlassian.util.concurrent.ThreadFactories;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class DefaultExecutorServiceFactoryBean implements ExecutorServiceFactoryBean
{
    private final String name;
    private final int size;

    public DefaultExecutorServiceFactoryBean()
    {
        this(2,"BitbucketConnectorExecutorServiceThread");
    }

    public DefaultExecutorServiceFactoryBean(int size, String name)
    {
        this.name = name;
        this.size = size;
    }

    @Override
    public Object getObject() throws Exception
    {
        return Executors.newFixedThreadPool(size, ThreadFactories.namedThreadFactory(name));
    }

    @Override
    public Class<ExecutorService> getObjectType()
    {
        return ExecutorService.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }
}
