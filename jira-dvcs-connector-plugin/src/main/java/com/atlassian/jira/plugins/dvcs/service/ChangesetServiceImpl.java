package com.atlassian.jira.plugins.dvcs.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.google.common.collect.Sets;

public class ChangesetServiceImpl implements ChangesetService
{
    
    private final ConcurrencyService concurrencyService;
    private final ChangesetDao changesetDao;
    private final DvcsCommunicatorProvider dvcsCommunicatorProvider;
    private final RepositoryDao repositoryDao;

    public ChangesetServiceImpl(ConcurrencyService concurrencyService, ChangesetDao changesetDao, DvcsCommunicatorProvider dvcsCommunicatorProvider, RepositoryDao repositoryDao)
    {
        this.concurrencyService = concurrencyService;
        this.changesetDao = changesetDao;
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
        this.repositoryDao = repositoryDao;
    }

    @Override
    public Changeset create(final Changeset changeset, final Set<String> extractedIssues)
    {
        return concurrencyService.synchronizedBlock(new ConcurrencyService.SynchronizedBlock<Changeset, RuntimeException>()
        {

            @Override
            public Changeset perform() throws RuntimeException
            {
                return changesetDao.create(changeset, extractedIssues);
            }

        }, Changeset.class, changeset.getRawNode());
    }

    @Override
    public Changeset update(Changeset changeset)
    {
        return changesetDao.update(changeset);
    }

    @Override
    public void removeAllInRepository(int repositoryId)
    {
        changesetDao.removeAllInRepository(repositoryId);
    }

    @Override
    public Iterable<Changeset> getChangesetsFromDvcs(Repository repository)
    {
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());
        return communicator.getChangesets(repository);
    }

    @Override
    public Changeset getDetailChangesetFromDvcs(Repository repository, Changeset changeset)
    {
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());
        return communicator.getDetailChangeset(repository, changeset);
    }

    @Override
    public List<Changeset> getByIssueKey(Iterable<String> issueKeys, boolean newestFirst)
    {
        List<Changeset> changesets = changesetDao.getByIssueKey(issueKeys, newestFirst);
        return checkChangesetVersion(changesets);
    }

    @Override
    public String getCommitUrl(Repository repository, Changeset changeset)
    {
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());
        return communicator.getCommitUrl(repository, changeset);
    }

    @Override
    public Map<ChangesetFile, String> getFileCommitUrls(Repository repository, Changeset changeset)
    {
        HashMap<ChangesetFile, String> fileCommitUrls = new HashMap<ChangesetFile, String>();
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());

        for (int i = 0;  i < changeset.getFiles().size(); i++)
        {
            ChangesetFile changesetFile = changeset.getFiles().get(i);
            String fileCommitUrl = communicator.getFileCommitUrl(repository, changeset, changesetFile.getFile(), i);

            fileCommitUrls.put(changesetFile, fileCommitUrl);
        }

        return fileCommitUrls;
    }

    @Override
    public Iterable<Changeset> getLatestChangesets(int maxResults, GlobalFilter gf)
    {
        List<Changeset> changesets = changesetDao.getLatestChangesets(maxResults, gf);
        checkChangesetVersion(changesets);
        return Sets.newHashSet(changesets);
    }

    @SuppressWarnings("unchecked")
    private List<Changeset> checkChangesetVersion(List<Changeset> changesets)
    {
        return (List<Changeset>) CollectionUtils.collect(changesets, new Transformer() {

            @Override
            public Object transform(Object input)
            {
                Changeset changeset = (Changeset) input;

                return ChangesetServiceImpl.this.checkChangesetVersion(changeset);
            }
        });
    }

    /**
     * Checks if changeset has latest version. If not it will be updated from remote DVCS and stored to DB.
     * Updated version will return back.
     *
     * @param changeset changeset on which we check version
     * @return updated changeset
     */
    private Changeset checkChangesetVersion(Changeset changeset)
    {
        if (changeset != null)
        {
            boolean isLatestVersion = changeset.getVersion() != null && changeset.getVersion() >= ChangesetMapping.LATEST_VERSION;

            if (!isLatestVersion)
            {
                Repository repository = repositoryDao.get(changeset.getRepositoryId());
                DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());

                Changeset updatedChangeset = communicator.getChangeset(repository, changeset.getNode());
                updatedChangeset = communicator.getDetailChangeset(repository, updatedChangeset);

                changeset.setRawAuthor(updatedChangeset.getRawAuthor());
                changeset.setAuthor(updatedChangeset.getAuthor());
                changeset.setDate(updatedChangeset.getDate());
                changeset.setRawNode(updatedChangeset.getRawNode());
                changeset.setParents(updatedChangeset.getParents());
                changeset.setFiles(updatedChangeset.getFiles());
                changeset.setAllFileCount(updatedChangeset.getAllFileCount());
                changeset.setAuthorEmail(updatedChangeset.getAuthorEmail());

                changeset = changesetDao.update(changeset);
            }
        }
        return changeset;
    }

	@Override
	public void markSmartcommitAvailability(int id, boolean available)
	{
		changesetDao.markSmartcommitAvailability(id, available);
	}

    @Override
    public Set<String> findReferencedProjects(int repositoryId)
    {
        return changesetDao.findReferencedProjects(repositoryId);
    }


}
