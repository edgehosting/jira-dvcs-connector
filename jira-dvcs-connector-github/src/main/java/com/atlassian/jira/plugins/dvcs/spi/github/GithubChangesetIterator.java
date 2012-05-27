package com.atlassian.jira.plugins.dvcs.spi.github;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;


public class GithubChangesetIterator implements Iterator<Changeset>
{
    private ListIterator<Changeset> inPageChangesetsIterator = Collections.<Changeset>emptyList().listIterator();
    private PagesIterator pagesIterator;

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

class PagesIterator implements Iterator<ListIterator<Changeset>>
{

    private final GithubCommunicator githubCommunicator;
    private final Repository repository;

    private int index = 0;
    private int currentPageNumber = 0;   // github gives us pages indexed from 1 (zero is one before)
    private final String branch;
    private List<Changeset> changesets;
    private boolean stoped = false;

    PagesIterator(String branch, GithubCommunicator githubCommunicator, Repository repository)
    {
        this.branch = branch;
        this.githubCommunicator = githubCommunicator;
        this.repository = repository;
    }

    @Override
    public boolean hasNext()
    {
        if (stoped) 
		{
            return false;
        }
        if (index < currentPageNumber) 
		{
            return containsChangesets();
        }
        currentPageNumber++;
        changesets = githubCommunicator.getChangesets(repository, branch, currentPageNumber);
        return containsChangesets();
    }

    private boolean containsChangesets() 
	{
        return changesets != null && !changesets.isEmpty();
    }

    @Override
    public ListIterator<Changeset> next()
    {
        index++;
        if (index != currentPageNumber && !hasNext()) {
            throw new NoSuchElementException();
        }
        if (changesets != null && !changesets.isEmpty()) {
            return changesets.listIterator();
        }
        return Collections.<Changeset>emptyList().listIterator();
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

class BranchesIterator implements Iterator<PagesIterator>
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
    public PagesIterator next()
    {
        if (!hasNext())
        {
            return null;
        }

        return new PagesIterator(branchNamesIterator.next(), githubCommunicator, repository);
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}

