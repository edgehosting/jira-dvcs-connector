package com.atlassian.jira.plugins.bitbucket.api.util;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 *
 */
public class Retryer<V>
{
    private static final int NUM_ATTEMPTS = 7;
    private static final Logger log = LoggerFactory.getLogger(Retryer.class);

	public V retry(Callable<V> callable)
	{
		// try few times
		for (int attempt = 1; attempt < NUM_ATTEMPTS; attempt++)
		{
			try
			{
				return callable.call();
			} catch (Exception e)
			{
				long delay = (long) (1000 * Math.pow(3, attempt)); // exponencial delay. (currently up to 12 minutes)
				log.warn("Attempt #" + attempt + " (out of " + NUM_ATTEMPTS
				        + "): Retrieving changesets failed: " + e.getMessage() + "\n. Retrying in "
				        + delay / 1000 + "secs");
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
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}