package com.atlassian.jira.plugins.dvcs.util;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
			} catch (Exception e)
			{
				long delay = (long) (1000 * Math.pow(3, attempt)); // exponencial delay. (currently up to 12 minutes)
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