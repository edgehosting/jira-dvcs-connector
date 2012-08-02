package com.atlassian.jira.plugins.dvcs.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

public class ChangesetServiceImpl implements ChangesetService
{
    private final ChangesetDao changesetDao;
    private final DvcsCommunicatorProvider dvcsCommunicatorProvider;
    private final RepositoryDao repositoryDao;

    public ChangesetServiceImpl(ChangesetDao changesetDao, DvcsCommunicatorProvider dvcsCommunicatorProvider, RepositoryDao repositoryDao)
    {
        this.changesetDao = changesetDao;
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
        this.repositoryDao = repositoryDao;
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
        List<Changeset> changesets = changesetDao.getByIssueKey(issueKey);
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
    public DvcsUser getUser(Repository repository, Changeset changeset)
    {
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());
        String username = changeset.getAuthor();
        
        if (StringUtils.isBlank(username))
        {
            return new DvcsUser(DvcsUser.UNKNOWN_USER.getUsername(), changeset.getRawAuthor(), DvcsUser.UNKNOWN_USER.getAvatar());
        }
        return communicator.getUser(repository, username);
    }

    @Override
    public String getUserUrl(Repository repository, Changeset changeset)
    {
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());
        return communicator.getUserUrl(repository, changeset);
    }

    @Override
    public Iterable<Changeset> getLatestChangesets(int maxResults, GlobalFilter gf)
    {
        List<Changeset> changesets = changesetDao.getLatestChangesets(maxResults, gf);
        checkChangesetVersion(changesets);
        return Sets.newHashSet(changesets);
    }

    private List<Changeset> checkChangesetVersion(List<Changeset> changesets)
    {
        Collection<Changeset> checkedChangesets = Collections2.transform(changesets,
                new Function<Changeset, Changeset>()
                {
                    @Override
                    public Changeset apply(Changeset changeset)
                    {
                        return checkChangesetVersion(changeset);
                    }
                });

        return new ArrayList<Changeset>(checkedChangesets);
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

                Changeset updatedChangeset = communicator.getDetailChangeset(repository, changeset);

                changeset.setRawAuthor(updatedChangeset.getRawAuthor());
                changeset.setAuthor(updatedChangeset.getAuthor());
                changeset.setDate(updatedChangeset.getDate());
                changeset.setRawNode(updatedChangeset.getRawNode());
                changeset.setParents(updatedChangeset.getParents());
                changeset.setFiles(updatedChangeset.getFiles());
                changeset.setAllFileCount(updatedChangeset.getAllFileCount());
                changeset.setAuthorEmail(updatedChangeset.getAuthorEmail());

                changeset = changesetDao.save(changeset);
            }
        }

        return changeset;
    }

	@Override
	public void markSmartcommitAvailability(int id, boolean available)
	{
		changesetDao.markSmartcommitAvailability(id, available);
	}
}
