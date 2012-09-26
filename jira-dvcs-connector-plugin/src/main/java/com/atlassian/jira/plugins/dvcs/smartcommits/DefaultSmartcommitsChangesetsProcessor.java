package com.atlassian.jira.plugins.dvcs.smartcommits;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.util.concurrent.ThreadFactories;

public class DefaultSmartcommitsChangesetsProcessor implements SmartcommitsChangesetsProcessor
{
	private final ThreadPoolExecutor executor;
	private final SmartcommitsService smartcommitService;
	private final CommitMessageParser commitParser;
	private final ChangesetDao changesetDao;
	
	public DefaultSmartcommitsChangesetsProcessor(ChangesetDao changesetDao, SmartcommitsService smartcommitService, CommitMessageParser commitParser)
	{
		this.changesetDao = changesetDao;
		this.smartcommitService = smartcommitService;
		this.commitParser = commitParser;

		executor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
		        ThreadFactories.namedThreadFactory(DefaultSmartcommitsChangesetsProcessor.class.getSimpleName()));
	}

	/**
	 * {@inheritDoc}
	 */
    @Override
    public void startProcess()
    {
        executor.execute(new SmartcommitOperation(changesetDao, commitParser, smartcommitService));
    }
	
}
