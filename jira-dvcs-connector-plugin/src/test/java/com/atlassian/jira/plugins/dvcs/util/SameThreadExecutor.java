package com.atlassian.jira.plugins.dvcs.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SameThreadExecutor extends ThreadPoolExecutor
{
    public SameThreadExecutor()
    {
        super(1, 1, 0L, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public void execute(Runnable task)
    {
        task.run();
    }

    @Override
    public Future<?> submit(Runnable task) {
        try
        {
            task.run();
            return new InstantFuture<Object>(null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task)
    {
        try
        {
            T result = task.call();
            return new InstantFuture<T>(result);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> Future<T> submit(Runnable task, T defaultResult) {
        try
        {
            task.run();
            return new InstantFuture<T>(defaultResult);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static class InstantFuture<T> implements Future<T> {
        private T result;

        public InstantFuture(T result) {
            this.result = result;
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning)
        {
            return false;
        }

        @Override
        public boolean isCancelled()
        {
            return false;
        }

        @Override
        public boolean isDone()
        {
            return true;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException
        {
            return result;
        }

        @Override
        public T get(final long timeout, final TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException
        {
            return result;
        }
    }
}
