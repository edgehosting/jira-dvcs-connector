package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.google.common.collect.Sets;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangesetServiceImpl implements ChangesetService
{
    private final ChangesetDao changesetDao;
    private final DvcsCommunicatorProvider dvcsCommunicatorProvider;

    public ChangesetServiceImpl(ChangesetDao changesetDao, DvcsCommunicatorProvider dvcsCommunicatorProvider)
    {
        this.changesetDao = changesetDao;
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
    }

    @Override
    public List<Changeset> getAllByIssue(String issueKey)
    {
        return null;
    }

    @Override
    public Changeset save(Changeset changeset)
    {
        return changesetDao.save(changeset);
    }


    @Override
    public void removeAllInRepository(int repositoryId)
    {
        changesetDao.removeAllInRepository(repositoryId);
    }

    @Override
    public Changeset getByNode(int repositoryId, String changesetNode)
    {
        return changesetDao.getByNode(repositoryId, changesetNode);
    }

    @Override
    public Iterable<Changeset> getChangesetsFromDvcs(Repository repository, Date lastCommitDate)
    {
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());

        return communicator.getChangesets(repository, lastCommitDate);
    }

    @Override
    public Changeset getDetailChangesetFromDvcs(Repository repository, Changeset changeset)
    {
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());

        return communicator.getDetailChangeset(repository, changeset);
    }

    @Override
    public List<Changeset> getByIssueKey(String issueKey)
    {
        return changesetDao.getByIssueKey(issueKey);
    }

    @Override
    public String getCommitUrl(Repository repository, Changeset changeset)
    {
        final DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());
        return communicator.getCommitUrl(repository, changeset);
    }

    @Override
    public Map<ChangesetFile, String> getFileCommitUrls(Repository repository, Changeset changeset)
    {
        final HashMap<ChangesetFile, String> fileCommitUrls = new HashMap<ChangesetFile, String>();
        final DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());

        for (int i = 0;  i < changeset.getFiles().size(); i++)
        {
            ChangesetFile changesetFile = changeset.getFiles().get(i);
            final String fileCommitUrl = communicator.getFileCommitUrl(repository, changeset, changesetFile.getFile(), i);

            fileCommitUrls.put(changesetFile, fileCommitUrl);
        }

        return fileCommitUrls;
    }

    @Override
    public DvcsUser getUser(Repository repository, Changeset changeset)
    {
        final DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());
        return communicator.getUser(repository, changeset.getAuthor());
    }

    @Override
    public String getUserUrl(Repository repository, Changeset changeset)
    {
        final DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());
        return communicator.getUserUrl(repository, changeset);
    }

    @Override
    public Iterable<Changeset> getLatestChangesets(int maxResults, GlobalFilter gf)
    {
        List<Changeset> changesets = changesetDao.getLatestChangesets(maxResults, gf);
        return Sets.newHashSet(changesets);
    }
}
