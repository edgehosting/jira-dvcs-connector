package com.atlassian.jira.plugins.dvcs.sync.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.ProgressWriter;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;

public class DefaultSynchronisationOperation implements SynchronisationOperation
{
    private static final Logger log = LoggerFactory.getLogger(DefaultSynchronisationOperation.class);

    protected final SynchronizationKey key;
    private final OrganizationService organizationService;
    protected final RepositoryService repositoryService;
    private final ProgressWriter progressProvider;
    private final ChangesetService changesetService;

    public DefaultSynchronisationOperation(SynchronizationKey key, OrganizationService organizationService,
                                           RepositoryService repositoryService, ChangesetService changesetService,
                                           ProgressWriter progressProvider)
    {
        this.key = key;
        this.organizationService = organizationService;
        this.repositoryService = repositoryService;
        this.changesetService = changesetService;
        this.progressProvider = progressProvider;
    }

    @Override
    public void synchronise()
    {

        final Repository repository = key.getRepository();
        final Organization organization = organizationService.get(repository.getOrganizationId(), false);

        Date lastCommitDate = null;
        if (key.isSoftSync())
        {
            lastCommitDate = repository.getLastCommitDate();
        } else
        {
            // we are doing full sync, lets delete all existing changesets
            changesetService.removeAllInRepository(repository.getId());
            repository.setLastCommitDate(null);
            repositoryService.save(repository);
        }

        int changesetCount = 0;
        int jiraCount = 0;
        int synchroErrorCount = 0;

        for (Changeset changeset : changesetService.getChangesetsFromDvcs(repository, lastCommitDate))
        {
            if (lastCommitDate == null || lastCommitDate.before(changeset.getDate()))
            {
                lastCommitDate = changeset.getDate();
                repository.setLastCommitDate(lastCommitDate);
                repositoryService.save(repository);
            }
            changesetCount++;
            String message = changeset.getMessage();
            log.debug("syncing changeset [{}] [{}]", changeset.getNode(), changeset.getMessage());

            Set<String> extractedIssues = extractIssueKeys(message);

            // get detial changeset because in this response is not information about files
            Changeset detailChangeset = null;
            if (CollectionUtils.isNotEmpty(extractedIssues))
            {
                try
                {
                    detailChangeset = changesetService.getDetailChangesetFromDvcs(organization, repository, changeset);
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
                        changesetService.save(changesetForSave);
                    } catch (SourceControlException e)
                    {
                        log.error("Error adding changeset " + changeset, e);
                    }
                }
            }
            progressProvider.inProgress(changesetCount, jiraCount, synchroErrorCount);
        }
    }

    private Set<String> extractIssueKeys(String message)
    {
        final String issueKeyRegex = "([A-Z][A-Z0-9]+-\\d+)";   //TODO check if we can use regexp from IssueLinkerImpl
        Pattern projectKeyPattern = Pattern.compile(issueKeyRegex, Pattern.CASE_INSENSITIVE);
        Matcher match = projectKeyPattern.matcher(message);

        Set<String> matches = new HashSet<String>();

        while (match.find())
        {
            // Get all groups for this match
            for (int i = 0; i <= match.groupCount(); i++)
            {
                String issueKey = match.group(i);
                matches.add(issueKey);
            }
        }
        return matches;
    }

}
