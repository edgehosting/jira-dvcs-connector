package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao.ForEachChangesetClosure;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommandsResults;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

import java.util.List;

/**
 * The Class RunnableChangesetSmartcommitProcessor.
 */
public class SmartcommitOperation implements Runnable
{
	
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(SmartcommitOperation.class);
	
	private final CommitMessageParser commitMessageParser;

	private final SmartcommitsService smartcommitsService;

	private final ChangesetDao changesetDao;

    private final Synchronizer synchronizer;

	/**
	 * The Constructor.
	 */
	public SmartcommitOperation(ChangesetDao changesetDao, CommitMessageParser commitMessageParser,
                                SmartcommitsService smartcommitsService, Synchronizer synchronizer)
	{
		this.changesetDao = changesetDao;
		this.commitMessageParser = commitMessageParser;
		this.smartcommitsService = smartcommitsService;
        this.synchronizer = synchronizer;
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
					log.debug("Processing message \n {} \n for smartcommits. Changeset id = {} node = {}.", new Object[]
					{ changesetMapping.getMessage(), changesetMapping.getID() , changesetMapping.getNode()});

					// first mark as processed
					changesetDao.markSmartcommitAvailability(changesetMapping.getID(), false);
					// parse message
					CommitCommands commands = commitMessageParser.parseCommitComment(changesetMapping.getMessage());
					commands.setCommitDate(changesetMapping.getDate());
					commands.setAuthorEmail(changesetMapping.getAuthorEmail());
					// do commands
					if (CollectionUtils.isNotEmpty(commands.getCommands())) {
                        final CommandsResults commandsResults = smartcommitsService.doCommands(commands);
                        if (commandsResults.hasErrors()) {
                            final List<RepositoryMapping> repositories = changesetDao.getRepositories(changesetMapping.getID());
                            for (RepositoryMapping repositoryMapping : repositories) {
                                final Progress progress = synchronizer.getProgress(repositoryMapping.getID());
                                if (progress != null) { // this repository has not been synchronized yet
                                    progress.setSmartCommitErrors(commandsResults.getAllErrors());
                                }
                            }

                        }
                    }
				}
			});
			
		} catch (Exception e)
		{
			log.warn("Failed to process smartcommit operation. Cause = " + e.getClass() + " : " + e.getMessage());
		}
	}

}
