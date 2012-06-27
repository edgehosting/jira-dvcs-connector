package com.atlassian.jira.plugins.dvcs.smartcommits;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.atlassian.util.concurrent.ThreadFactories;

public class DefaultSmartcommitsChangesetsProcessor implements SmartcommitsChangesetsProcessor
{
	private final ThreadPoolExecutor executor;

	public DefaultSmartcommitsChangesetsProcessor()
	{
		executor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
		        ThreadFactories.namedThreadFactory(DefaultSmartcommitsChangesetsProcessor.class.getSimpleName()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void queue(SmartcommitOperation operation) {

		executor.execute(operation);
		
	}
}

