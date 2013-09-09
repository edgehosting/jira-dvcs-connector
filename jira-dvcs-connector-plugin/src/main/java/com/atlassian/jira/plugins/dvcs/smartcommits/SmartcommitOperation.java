package com.atlassian.jira.plugins.dvcs.smartcommits;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao.ForEachChangesetClosure;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.SmartCommitError;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommandsResults;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;

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
    private final Repository repository;
    private final ChangesetService changesetService;

    /**
     * The Constructor.
     */
    public SmartcommitOperation(ChangesetDao changesetDao, CommitMessageParser commitMessageParser,
                                SmartcommitsService smartcommitsService, Synchronizer synchronizer, Repository repository, ChangesetService changesetService)
    {
        this.changesetDao = changesetDao;
        this.commitMessageParser = commitMessageParser;
        this.smartcommitsService = smartcommitsService;
        this.synchronizer = synchronizer;
        this.repository = repository;
        this.changesetService = changesetService;
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

            changesetDao.forEachLatestChangesetsAvailableForSmartcommitDo(repository.getId(), new ForEachChangesetClosure()
            {
                @Override
                public void execute(Changeset changeset)
                {
                    log.debug("Processing message \n {} \n for smartcommits. Changeset id = {} node = {}.", new Object[]
                            {changeset.getMessage(), changeset.getId(), changeset.getNode()});

                    // first mark as processed
                    changesetDao.markSmartcommitAvailability(changeset.getId(), false);
                    // parse message
                    CommitCommands commands = commitMessageParser.parseCommitComment(changeset.getMessage());
                    commands.setCommitDate(changeset.getDate());
                    commands.setAuthorEmail(changeset.getAuthorEmail());
                    // do commands
                    if (CollectionUtils.isNotEmpty(commands.getCommands()))
                    {
                        final CommandsResults commandsResults = smartcommitsService.doCommands(commands);
                        if (commandsResults.hasErrors())
                        {

                            final String commitUrl = changesetService.getCommitUrl(repository, changeset);

                            final Progress progress = synchronizer.getProgress(repository.getId());

                            List<SmartCommitError> smartCommitErrors = new ArrayList<SmartCommitError>();
                            for (String error : commandsResults.getAllErrors())
                            {
                                SmartCommitError sce = new SmartCommitError(changeset.getNode(), commitUrl, error);
                                smartCommitErrors.add(sce);
                            }
                            progress.setSmartCommitErrors(smartCommitErrors);
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
