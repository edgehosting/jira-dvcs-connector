package com.atlassian.jira.plugins.dvcs.sync.impl;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Set;

public class DefaultSynchronisationOperation implements SynchronisationOperation
{
    private static final Logger log = LoggerFactory.getLogger(DefaultSynchronisationOperation.class);

    protected final Repository repository;
    protected final RepositoryService repositoryService;
    private final DefaultProgress progress;
    private final ChangesetService changesetService;
    private final boolean softSync;

    /**
     * performance improvement for syncing GH large repositories with changesets which doesn't contain issue keys.
     * changeset after this step will be stored even if it has no issue key
     */
    private static int GH_CHANGESETS_SAVING_INTERVAL = 100;

    public DefaultSynchronisationOperation(Repository repository, RepositoryService repositoryService, ChangesetService changesetService,
        boolean softSync)
    {
        this.repository = repository;
        this.repositoryService = repositoryService;
        this.changesetService = changesetService;
        progress = new DefaultProgress();
        this.softSync = softSync;
    }



    @Override
    public void synchronise()
    {
        Date lastCommitDate = repository.getLastCommitDate();
        synchroniseInternal();

        if (!ObjectUtils.equals(lastCommitDate, repository.getLastCommitDate()))
        {
            repositoryService.save(repository);
        }
    }



    public void synchroniseInternal()
    {
        Date lastCommitDate = null;

        if (softSync)
        {
            lastCommitDate = repository.getLastCommitDate();
        } else
        {
            // we are doing full sync, lets delete all existing changesets
            changesetService.removeAllInRepository(repository.getId());
            repository.setLastCommitDate(null);
        }

        int changesetCount = 0;
        int jiraCount = 0;
        int synchroErrorCount = 0;

        Iterable<Changeset> allOrLatestChangesets = changesetService.getChangesetsFromDvcs(repository, lastCommitDate);



        for (Changeset changeset : allOrLatestChangesets)
        {
        	if (progress.isShouldStop())
        	{
        		return;
        	}
            if (lastCommitDate == null || lastCommitDate.before(changeset.getDate()))
            {
                lastCommitDate = changeset.getDate();
                repository.setLastCommitDate(lastCommitDate);
            }
            changesetCount++;
            String message = changeset.getMessage();
            log.debug("syncing changeset [{}] [{}]", changeset.getNode(), changeset.getMessage());

            Set<String> extractedIssues = extractIssueKeys(message);


            // see GH_CHANGESETS_SAVING_INTERVAL javadoc
            if ("github".equals(repository.getDvcsType()) &&
                    (changesetCount % GH_CHANGESETS_SAVING_INTERVAL) == 0 &&
                    CollectionUtils.isEmpty(extractedIssues))
            {
                changeset.setIssueKey("NON_EXISTING-0");
                changesetService.save(changeset);
                continue;
            }

            // get detail changeset because in this response is not information about files
            Changeset detailChangeset = null;
            if (CollectionUtils.isNotEmpty(extractedIssues))
            {
                try
                {
                    detailChangeset = changesetService.getDetailChangesetFromDvcs(repository, changeset);
                } catch (Exception e)
                {
                    log.warn("Unable to retrieve details for changeset " + changeset.getNode(), e);
                    synchroErrorCount++;
                }
                for (String extractedIssue : extractedIssues)
                {
                    jiraCount++;
                    String issueKey = extractedIssue.toUpperCase();
                    try
                    {
                        Changeset changesetForSave = detailChangeset == null ? changeset : detailChangeset;
                        changesetForSave.setIssueKey(issueKey);
                        //--------------------------------------------
                        // mark smart commit can be processed
                        if (softSync) {
                        	markChangesetForSmartCommit(changesetForSave);
                        }
                        //--------------------------------------------
                        changesetService.save(changesetForSave);
                    } catch (SourceControlException e)
                    {
                        log.error("Error adding changeset " + changeset, e);
                    }
                }
            }
            progress.inProgress(changesetCount, jiraCount, synchroErrorCount);
        }
    }

	private void markChangesetForSmartCommit(Changeset changesetForSave)
	{
		if (repository.isSmartcommitsEnabled()) {
			
			log.debug("Marking changeset node = {} to be processed by smart commits", changesetForSave.getRawNode());
			
			changesetForSave.setSmartcommitAvaliable(Boolean.TRUE);

		}
	}

	private Set<String> extractIssueKeys(String message)
    {
        return IssueKeyExtractor.extractIssueKeys(message);
    }

    @Override
    public DefaultProgress getProgress()
    {
        return progress;
    }

    @Override
    public boolean isSoftSync()
    {
        return softSync;
    }
    
}
