package com.atlassian.jira.plugins.bitbucket;

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
        this(2,"ExecutorServiceThread");
    }

    public DefaultExecutorServiceFactoryBean(int size, String name)
    {
        this.name = name;
        this.size = size;
    }

    public Object getObject() throws Exception
    {
        return Executors.newFixedThreadPool(size, ThreadFactories.namedThreadFactory(name));
    }

    public Class getObjectType()
    {
        return ExecutorService.class;
    }

    public boolean isSingleton()
    {
        return true;
    }
}
