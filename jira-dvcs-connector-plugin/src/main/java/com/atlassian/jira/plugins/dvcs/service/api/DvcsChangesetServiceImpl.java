package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@ExportAsService (DvcsChangesetService.class)
@Component
public class DvcsChangesetServiceImpl implements DvcsChangesetService
{
    private ChangesetService changesetService;

    @Autowired
    public DvcsChangesetServiceImpl(ChangesetService changesetService)
    {
        this.changesetService = changesetService;
    }

    @Override
    public List<Changeset> getChangesets(Repository repository)
    {
        return ImmutableList.copyOf(changesetService.getChangesets(repository));
    }

    @Override
    public List<Changeset> getChangesets(Iterable<String> issueKeys)
    {
        return ImmutableList.copyOf(changesetService.getByIssueKey(issueKeys, false));
    }

    @Override
    public List<Changeset> getChangesets(Iterable<String> issueKeys, String dvcsType)
    {
        return ImmutableList.copyOf(changesetService.getByIssueKey(issueKeys, dvcsType, false));
    }

    @Override
    public List<Changeset> getChangesets(int maxResults, GlobalFilter globalFilter)
    {
        return ImmutableList.copyOf(changesetService.getLatestChangesets(maxResults, globalFilter));
    }

    @Override
    public String getChangesetURL(Repository repository, Changeset changeset)
    {
        return changesetService.getCommitUrl(repository, changeset);
    }

    @Override
    public Map<ChangesetFile, String> getFileChangesets(Repository repository, Changeset changeset)
    {
        return changesetService.getFileCommitUrls(repository, changeset);
    }

    @Override
    public List<Changeset> getChangesetsWithFileDetails(final List<Changeset> changesets)
    {
        return changesetService.getChangesetsWithFileDetails(changesets);
    }
}
