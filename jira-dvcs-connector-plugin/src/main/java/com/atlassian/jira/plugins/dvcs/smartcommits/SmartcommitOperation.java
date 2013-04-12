package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao.ForEachChangesetClosure;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

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
			log.debug("Running SmartcommitOperation ... ");

			changesetDao.forEachLatestChangesetsAvailableForSmartcommitDo(new ForEachChangesetClosure()
			{
				@Override
				public void execute(ChangesetMapping changesetMapping)
				{
					log.debug("Processing message \n {} \n for smartcommits. Changeset id = {}.", new Object[]
					{ changesetMapping.getMessage(), changesetMapping.getID() });

					// first mark as processed
					changesetDao.markSmartcommitAvailability(changesetMapping.getID(), false);
					// parse message
					CommitCommands commands = commitMessageParser.parseCommitComment(changesetMapping.getMessage());
					commands.setCommitDate(changesetMapping.getDate());
					commands.setAuthorEmail(changesetMapping.getAuthorEmail());
					// do commands
					if (CollectionUtils.isNotEmpty(commands.getCommands())) {
						smartcommitsService.doCommands(commands);
					}
				}
			});
			
		} catch (Exception e)
		{
			log.warn("Failed to process smartcommit operation. Cause = " + e.getClass() + " : " + e.getMessage());
		}
	}

}
