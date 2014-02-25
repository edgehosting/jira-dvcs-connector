package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Progress;

import java.util.Date;

/**
 * Interceptor to compute flight times for requests
 *
 */
public class FlightTimeInterceptor
{
    public static <T, E extends Throwable> T execute(Progress progress, ThrowableCallable<T, E> callable) throws E
    {
        if (progress != null)
        {
            progress.incrementRequestCount(new Date());
        }

        final long startFlightTime = System.currentTimeMillis();

        T result = null;
        try
        {
            result = callable.call();
        }
        finally
        {
            if (progress != null)
            {
                progress.addFlightTimeMs((int) (System.currentTimeMillis() - startFlightTime));
            }
        }

        return result;
    }

    public static interface Callable<V> extends ThrowableCallable<V, RuntimeException>
    {

    }

    public static interface ThrowableCallable<V, E extends Throwable>
    {
        /**
         * Returns a result or throws E if unable to execute.
         *
         * @return result
         * @throws E if unable to execute
         */
        V call() throws E;
    }
}