package com.atlassian.jira.plugins.dvcs.sync.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.dao.impl.ChangesetDaoImpl;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;

public class DefaultSynchronisationOperation implements SynchronisationOperation
{
    private static final Logger log = LoggerFactory.getLogger(DefaultSynchronisationOperation.class);

    private final Repository repository;
    private final RepositoryService repositoryService;
    private final DefaultProgress progress;
    private final ChangesetService changesetService;
    private final boolean softSync;

    private final DvcsCommunicator communicator;

    public DefaultSynchronisationOperation(DvcsCommunicator communicator, Repository repository, RepositoryService repositoryService, ChangesetService changesetService,
        boolean softSync)
    {
        this.communicator = communicator;
        this.repository = repository;
        this.repositoryService = repositoryService;
        this.changesetService = changesetService;
        this.progress = new DefaultProgress();
        this.softSync = softSync;
    }

    @Override
    public void synchronise()
    {
        Date lastCommitDate = repository.getLastCommitDate();
        synchroniseInternal();

        if (!ObjectUtils.equals(lastCommitDate, repository.getLastCommitDate()))
        {
            log.debug("Last commit date has been changed. Save repository: [{}]", repository);
            repositoryService.save(repository);
        }
    }

    public void synchroniseInternal()
    {
        Date lastCommitDate = null;

        if (!softSync)
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

        Iterable<Changeset> allOrLatestChangesets = changesetService.getChangesetsFromDvcs(repository);

        Set<String> foundProjectKeys = new HashSet<String>();

        boolean lastChangesetNodeUpdated = false;
        for (Changeset changeset : allOrLatestChangesets)
        {
        	if (progress.isShouldStop())
        	{
        		return;
        	}
            if (!lastChangesetNodeUpdated)
            {
                repository.setLastChangesetNode(changeset.getRawNode());
                lastChangesetNodeUpdated = true;
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
            //final boolean github = "github".equals(repository.getDvcsType());
            if ( /*github &&*/ CollectionUtils.isEmpty(extractedIssues) ) // storing only issues without issueKeys as
                // those would be stored below
            {
                changeset.setIssueKey("NON_EXISTING-0");
                changesetService.save(changeset);
                continue;
            }

            // get detail changeset because in this response is not information about files
            Changeset detailChangeset = null;
            
            if (CollectionUtils.isNotEmpty(extractedIssues) ) 
            {
                if ( /*github*/ "github".equals(repository.getDvcsType()) )
                {
                    // we have requested detail changesets for github
                    detailChangeset = changeset;
                } else
                {
                    try
                    {
                        detailChangeset = changesetService.getDetailChangesetFromDvcs(repository, changeset);
                    } catch (Exception e)
                    {
                        log.warn("Unable to retrieve details for changeset " + changeset.getNode(), e);
                        synchroErrorCount++;
                    }
                }
                
                boolean changesetAlreadyMarkedForSmartCommits = false;
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
                        // + store extracted project key for incremental linking
                        if (softSync && !changesetAlreadyMarkedForSmartCommits)
                        {
                            markChangesetForSmartCommit(changesetForSave, true);
                            changesetAlreadyMarkedForSmartCommits = true;
                        } else
                        {
                            markChangesetForSmartCommit(changesetForSave, false);
                        }
                        
                        foundProjectKeys.add(ChangesetDaoImpl.parseProjectKey(issueKey));
                        //--------------------------------------------
                        log.debug("Save changeset [{}]", changesetForSave);
                        changesetService.save(changesetForSave);
                    
                    } catch (SourceControlException e)
                    {
                        log.error("Error adding changeset " + changeset, e);
                    }
                }
            }
            progress.inProgress(changesetCount, jiraCount, synchroErrorCount);
        }
        
        setupNewLinkers(foundProjectKeys);
    }
    
    private void setupNewLinkers(Set<String> extractedProjectKeys)
    {
        if (!extractedProjectKeys.isEmpty())
        {
            if (softSync)
            {
                communicator.linkRepositoryIncremental(repository, extractedProjectKeys);
            } else
            {
                communicator.linkRepository(repository, extractedProjectKeys);
            }
        }
    }

    private void markChangesetForSmartCommit(Changeset changesetForSave, boolean mark)
	{
        if (repository.isSmartcommitsEnabled())
        {
            log.debug("Marking changeset node = {} to be processed by smart commits", changesetForSave.getRawNode());
            changesetForSave.setSmartcommitAvaliable(mark);
        } else {
        	log.debug("Changeset node = {}. Repository not enabled for smartcommits.", changesetForSave.getRawNode());
        }
	}

	private Set<String> extractIssueKeys(String message)
    {
	    // TODO check if these issues actually exists...
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
