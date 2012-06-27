package com.atlassian.jira.plugins.dvcs.smartcommits;

import java.util.List;

import org.slf4j.Logger;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;

/**
 * The Class RunnableChangesetSmartcommitProcessor.
 */
public class SmartcommitOperation implements Runnable
{
	
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(SmartcommitOperation.class);
	
	private final ChangesetService changesetService;

	private final CommitMessageParser commitMessageParser;

	private final SmartcommitsService smartcommitsService;

	/**
	 * The Constructor.
	 */
	public SmartcommitOperation(ChangesetService changesetService, CommitMessageParser commitMessageParser, SmartcommitsService smartcommitsService)
	{
		super();
		this.changesetService = changesetService;
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
			List<Changeset> latestChangesetsAvailableForSmartcommits = changesetService.getLatestChangesetsAvailableForSmartcommits();
			for (Changeset changeset : latestChangesetsAvailableForSmartcommits)
			{
				// first mark as processed
				changesetService.markSmartcommitAvailability(changeset.getId(), true);
				// parse message
				CommitCommands commands = commitMessageParser.parseCommitComment(changeset.getMessage());
				commands.setAuthorEmail(changeset.getAuthorEmail());
				// do commands
				smartcommitsService.doCommands(commands);
			}
			
		} catch (Exception e)
		{
			log.warn("Failed to process smartcommit operation. Cause message = " + e.getMessage());
		}
	}

}
