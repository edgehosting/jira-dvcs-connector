package com.atlassian.jira.plugins.dvcs.smartcommits;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao.ForEachChangesetClosure;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;

/**
 * The Class RunnableChangesetSmartcommitProcessor.
 */
public class SmartcommitOperation implements Runnable
{
	
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(SmartcommitOperation.class);
	
	private final CommitMessageParser commitMessageParser;

	private final SmartcommitsService smartcommitsService;

	private final ChangesetDao changesetDao;

	/**
	 * The Constructor.
	 */
	public SmartcommitOperation(ChangesetDao changesetDao, CommitMessageParser commitMessageParser, SmartcommitsService smartcommitsService)
	{
		super();
		this.changesetDao = changesetDao;
		this.commitMessageParser = commitMessageParser;
		this.smartcommitsService = smartcommitsService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run()
	{
		try
		{
			changesetDao.forEachLatestChangesetsAvailableForSmartcommitDo(new ForEachChangesetClosure()
			{
				@Override
				public void execute(Changeset changeset)
				{
					// first mark as processed 
					changesetDao.markSmartcommitAvailability(changeset.getId(), false);
					// parse message
					CommitCommands commands = commitMessageParser.parseCommitComment(changeset.getMessage());
					commands.setAuthorEmail(changeset.getAuthorEmail());
					// do commands
					if (CollectionUtils.isNotEmpty(commands.getCommands())) {
						smartcommitsService.doCommands(commands);
					}
				}
			});
			
		} catch (Exception e)
		{
			log.warn("Failed to process smartcommit operation. Cause message = " + e.getMessage());
		}
	}

}
