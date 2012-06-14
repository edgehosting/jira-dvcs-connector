package com.atlassian.jira.plugins.dvcs.spi.github;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.PageIterator;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.spi.github.parsers.GithubChangesetFactory;


public class GithubChangesetIterator implements Iterator<Changeset>
{
    private ListIterator<Changeset> inPageChangesetsIterator = Collections.<Changeset>emptyList().listIterator();
    private StopablePageIterator pagesIterator;

    private final BranchesIterator branchesIterator;

    private final Repository repository;

    private Changeset nextChangeset = null;
    private final Date lastCommitDate;
	private ChangesetCache changesetCache;

    public GithubChangesetIterator(ChangesetCache changesetCache, GithubCommunicator githubCommunicator,
                                   Repository repository, List<String> branches, Date lastCommitDate)
    {
        this.changesetCache = changesetCache;
        this.repository = repository;
        this.lastCommitDate = lastCommitDate;

        branchesIterator = new BranchesIterator(branches, githubCommunicator, repository);
        pagesIterator = branchesIterator.next();
    }

    @Override
    public boolean hasNext()
    {
        final boolean hasNext = inPageChangesetsIterator.hasNext() || (pagesIterator != null && pagesIterator.hasNext()) || branchesIterator.hasNext();
        if (hasNext)
        {
            nextChangeset = internalNext();
            if (shoudStopBranchIteration())
            {
                inPageChangesetsIterator = Collections.<Changeset>emptyList().listIterator();
                pagesIterator.stop();
                return hasNext();
            }

        }
        return hasNext;
    }

    private boolean shoudStopBranchIteration()
    {
        boolean changesetOlderThanLastCommitDate = lastCommitDate != null && lastCommitDate.after(nextChangeset.getDate());
        boolean changesetAlreadySynchronized = changesetCache.isCached(repository.getId(), nextChangeset.getNode());
        return changesetOlderThanLastCommitDate || changesetAlreadySynchronized;
    }
 
    private Changeset internalNext() {
        if (inPageChangesetsIterator.hasNext())
        {
            return inPageChangesetsIterator.next();
        } else if (pagesIterator.hasNext())
        {
            inPageChangesetsIterator = pagesIterator.next();
            return internalNext();
        } else if (branchesIterator.hasNext())
        {
            pagesIterator = branchesIterator.next();
            return internalNext();
        }

        throw new NoSuchElementException();

    }

    @Override
    public Changeset next()
    {
        if (nextChangeset == null)
        {
            nextChangeset = internalNext();
        }

        Changeset changeset = nextChangeset;
        nextChangeset = null;
        return changeset;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}

class StopablePageIterator implements Iterator<ListIterator<Changeset>>
{
    private final PageIterator<RepositoryCommit> pageIterator;
    private final Repository repository;
    private final String branch;

    private boolean stoped = false;

    StopablePageIterator(PageIterator<RepositoryCommit> pageIterator, Repository repository, String branch)
    {
        this.pageIterator = pageIterator;
        this.repository = repository;
        this.branch = branch;
    }
    
    @Override
    public boolean hasNext()
    {
        return !stoped && pageIterator.hasNext();
    }
    

    @Override
    public ListIterator<Changeset> next()
    {
        final ArrayList<Changeset> changesets = new ArrayList<Changeset>();
        final Collection<RepositoryCommit> page = pageIterator.next();
        for (Object obj : page)
        {
            RepositoryCommit repositoryCommit = (RepositoryCommit) obj;
            Changeset changeset = GithubChangesetFactory.transform(repositoryCommit, repository.getId(), branch);
            changesets.add(changeset);
        }
        return changesets.listIterator();
    }


    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    public void stop() 
	{
        this.stoped = true;
    }
}

class BranchesIterator implements Iterator<StopablePageIterator>
{

    private ListIterator<String> branchNamesIterator = Collections.<String>emptyList().listIterator();
    private final GithubCommunicator githubCommunicator;
    private final Repository repository;

    BranchesIterator(List<String> branches, GithubCommunicator githubCommunicator, Repository repository)
    {
        this.githubCommunicator = githubCommunicator;
        this.repository = repository;
        this.branchNamesIterator = branches.listIterator();
    }

    @Override
    public boolean hasNext()
    {
        return branchNamesIterator.hasNext();
    }

    @Override
    public StopablePageIterator next()
    {
        if (!hasNext())
        {
            return null;
        }

        final String branch = branchNamesIterator.next();
        return new StopablePageIterator(githubCommunicator.getPageIterator(repository, branch), repository, branch);
       }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}

