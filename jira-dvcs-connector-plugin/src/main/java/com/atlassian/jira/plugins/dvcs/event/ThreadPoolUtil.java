package com.atlassian.jira.plugins.dvcs.event;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to create ThreadPoolExecutor instances.
 */
public class ThreadPoolUtil
{
    public static ThreadPoolExecutor newSingleThreadExecutor(ThreadFactory threadFactory)
    {
        return new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                threadFactory);
    }
}
