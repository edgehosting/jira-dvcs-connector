package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

import java.util.Date;
import java.util.List;

public class ChangesetServiceImpl implements ChangesetService
{

    private ChangesetDao changesetDao;
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;
    private OrganizationService organizationService;

    public ChangesetServiceImpl(ChangesetDao changesetDao, DvcsCommunicatorProvider dvcsCommunicatorProvider, OrganizationService organizationService)
    {
        this.changesetDao = changesetDao;
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
        this.organizationService = organizationService;
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
    public void removeAll(int repositoryId)
    {
    }

    @Override
    public void removeAllInRepository(int repositoryId)
    {
        changesetDao.removeAllInRepository(repositoryId);
    }

    @Override
    public Iterable<Changeset> getChangesetsFromDvcs(Repository repository, Date lastCommitDate)
    {
        final Organization organization = organizationService.get(repository.getOrganizationId(), false);
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());

        return communicator.getChangesets(organization, repository, lastCommitDate);
    }

    @Override
    public Changeset getDetailChangesetFromDvcs(Organization organization, Repository repository, Changeset changeset)
    {
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());

        return communicator.getDetailChangeset(organization, repository, changeset);
    }
}
