package com.atlassian.jira.plugins.dvcs.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetailsEnvelope;
import com.atlassian.jira.plugins.dvcs.model.Changesets;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangesetServiceImpl implements ChangesetService
{
    private static final Logger logger = LoggerFactory.getLogger(ChangesetServiceImpl.class);
    private final ConcurrencyService concurrencyService;

    private final ChangesetDao changesetDao;

    @Resource
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Resource
    private RepositoryDao repositoryDao;

    public ChangesetServiceImpl(ConcurrencyService concurrencyService, ChangesetDao changesetDao)
    {
        this.concurrencyService = concurrencyService;
        this.changesetDao = changesetDao;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Changeset getByNode(int repositoryId, String changesetNode)
    {
        return changesetDao.getByNode(repositoryId, changesetNode);
    }

    @Override
    public List<Changeset> getChangesets(Repository repository)
    {
        return changesetDao.getByRepository(repository.getId());
    }

    @Override
    public List<Changeset> getChangesetsWithFileDetails(List<Changeset> changesets)
    {
        ImmutableList.Builder<Changeset> detailedChangesets = ImmutableList.builder();

        // group by repo so we only have to load each repo one time inside the loop
        ListMultimap<Integer, Changeset> changesetsByRepo = Multimaps.index(changesets, Changesets.TO_REPOSITORY_ID);
        for (Map.Entry<Integer, Collection<Changeset>> repoChangesets : changesetsByRepo.asMap().entrySet())
        {
            final Repository repository = repositoryDao.get(repoChangesets.getKey());
            final DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());

            for (Changeset changeset : repoChangesets.getValue())
            {
                if (changeset.getFileDetails() == null)
                {
                    try
                    {
                        ChangesetFileDetailsEnvelope changesetFileDetailsEnvelope = communicator.getFileDetails(repository, changeset);
                        List<ChangesetFileDetail> fileDetails = changesetFileDetailsEnvelope.getFileDetails();
                        logger.debug("Loaded file details for {}: {}", changeset, fileDetails);

                        changeset.setAllFileCount(changesetFileDetailsEnvelope.getCount());

                        fileDetails = fileDetails.subList(0, Math.min(fileDetails.size(), Changeset.MAX_VISIBLE_FILES));

                        // keep these two in sync
                        changeset.setFiles(ImmutableList.<ChangesetFile>copyOf(fileDetails));
                        changeset.setFileDetails(fileDetails);
                        changeset = changesetDao.update(changeset);
                    }
                    catch (SourceControlException e)
                    {
                        logger.debug("Error getting file details for: " + changeset, e);
                    }
                }

                detailedChangesets.add(changeset);
            }
        }

        return detailedChangesets.build();
    }

    @Override
    public List<Changeset> getByIssueKey(Iterable<String> issueKeys, boolean newestFirst)
    {
        List<Changeset> changesets = changesetDao.getByIssueKey(issueKeys, newestFirst);
        return checkChangesetVersion(changesets);
    }

    @Override
    public List<Changeset> getByIssueKey(Iterable<String> issueKeys, String dvcsType, boolean newestFirst)
    {
        List<Changeset> changesets = changesetDao.getByIssueKey(issueKeys, dvcsType, newestFirst);
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
        if (changeset.getFiles() != null)
        {
            DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());

            for (int i = 0;  i < changeset.getFiles().size(); i++)
            {
                ChangesetFile changesetFile = changeset.getFiles().get(i);
                String fileCommitUrl = communicator.getFileCommitUrl(repository, changeset, changesetFile.getFile(), i);

                fileCommitUrls.put(changesetFile, fileCommitUrl);
            }
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

                changeset.setRawAuthor(updatedChangeset.getRawAuthor());
                changeset.setAuthor(updatedChangeset.getAuthor());
                changeset.setDate(updatedChangeset.getDate());
                changeset.setRawNode(updatedChangeset.getRawNode());
                changeset.setParents(updatedChangeset.getParents());
                changeset.setFiles(updatedChangeset.getFiles());
                changeset.setAllFileCount(updatedChangeset.getAllFileCount());
                changeset.setAuthorEmail(updatedChangeset.getAuthorEmail());
                changeset.setFileDetails(updatedChangeset.getFileDetails());

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
