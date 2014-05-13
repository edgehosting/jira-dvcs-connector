package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException.RetryableRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;


/**
 * Bitbucket occasionally randomly returns 400. 
 * Replying same request again usually returns correct response.
 * All requests that caused exception marked as {@link RetryableRequestException} will be retried
 */
public class BadRequestRetryer<V>
{
    private static final int DEFAULT_NUM_ATTEMPTS = 2;
    private static final Logger log = LoggerFactory.getLogger(BadRequestRetryer.class);

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
            } catch (RuntimeException e)
            {
                if ( e instanceof RetryableRequestException)
                {
                    long delay = (long) (1000 * Math.pow(3, attempt)); // exponential delay
                    log.warn("Attempt #" + attempt + " (out of " + num_attempts
                            + "): Request operation failed: " + e.getMessage() + "\nRetrying in "
                            + delay / 1000 + " secs");
                    try
                    {
                        Thread.sleep(delay);
                    } catch (InterruptedException ignored)
                    {
                        // ignore
                    }
                } else
                {
                    throw e;
                }
            } catch (Exception e)
            {
                throw new RuntimeException(e);
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
