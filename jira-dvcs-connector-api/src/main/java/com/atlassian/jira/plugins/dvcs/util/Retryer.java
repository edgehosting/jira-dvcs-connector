package com.atlassian.jira.plugins.dvcs.util;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;


/**
 */
public class Retryer<V>
{
    private static final int DEFAULT_NUM_ATTEMPTS = 3;
    private static final Logger log = LoggerFactory.getLogger(Retryer.class);

    public V retry(Callable<V> callable)
    {
        return retry(callable, DEFAULT_NUM_ATTEMPTS);
    }
    
    public V retry(Callable<V> callable, int num_attempts)
    {
        // try few times
        for (int attempt = 1; attempt < num_attempts; attempt++)
        {
            try
            {
                return callable.call();
            } catch (SourceControlException e)
            {
                throw e;
            } catch (Exception e)
            {
                long delay = (long) (1000 * Math.pow(3, attempt)); // exponential delay.
                log.warn("Attempt #" + attempt + " (out of " + num_attempts
                        + "): Retrieving operation failed: " + e.getMessage() + "\nRetrying in "
                        + delay / 1000 + " secs");
                try
                {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored)
                {
                    // ignore
                }
            }
        }

        // previous tries failed, let's go for it one more final time
        try
        {
            return callable.call();
        } catch (RuntimeException e)
        {
            throw e;
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
