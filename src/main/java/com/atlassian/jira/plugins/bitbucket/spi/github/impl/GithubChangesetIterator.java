package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class GithubChangesetIterator implements Iterator<Changeset>
{
    private ListIterator<Changeset> inPageChangesetsIterator = Collections.<Changeset>emptyList().listIterator();
    private PagesIterator pagesIterator;

    private BranchesIterator branchesIterator;
    private GithubRepositoryManager githubRepositoryManager;
    private SourceControlRepository repository;

    private Changeset nextChangeset = null;

    public GithubChangesetIterator(GithubRepositoryManager repositoryManager, GithubCommunicator githubCommunicator, SourceControlRepository repository, List<String> branches)
    {
        this.githubRepositoryManager = repositoryManager;
        this.repository = repository;

        branchesIterator = new BranchesIterator(branches, githubCommunicator, repository);
        pagesIterator = branchesIterator.next();
    }

    public boolean hasNext()
    {
        final boolean hasNext = inPageChangesetsIterator.hasNext() || (pagesIterator != null && pagesIterator.hasNext()) || branchesIterator.hasNext();
        if (hasNext)
        {
            nextChangeset = internalNext();
            if (githubRepositoryManager.wasChangesetAlreadySynchronized(repository.getId(), nextChangeset.getNode()))
            {
                inPageChangesetsIterator = Collections.<Changeset>emptyList().listIterator();
                pagesIterator.stop();
                return hasNext();
            }

        }
        return hasNext;
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

    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}

class PagesIterator implements Iterator<ListIterator<Changeset>>
{

    private GithubCommunicator githubCommunicator;
    private SourceControlRepository repository;

    private int index = 0;
    private int currentPageNumber = 0;   // github gives us pages indexed from 1 (zero is one before)
    private String branch;
    private List<Changeset> changesets;
    private boolean stoped = false;

    PagesIterator(String branch, GithubCommunicator githubCommunicator, SourceControlRepository repository)
    {
        this.branch = branch;
        this.githubCommunicator = githubCommunicator;
        this.repository = repository;
    }

    @Override
    public boolean hasNext()
    {
        if (stoped) {
            return false;
        }
        if (index < currentPageNumber) {
            return containsChangesets();
        }
        currentPageNumber++;
        changesets = githubCommunicator.getChangesets(repository, branch, currentPageNumber);
        return containsChangesets();
    }

    private boolean containsChangesets() {
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

    public void stop() {
        this.stoped = true;
    }
}

class BranchesIterator implements Iterator<PagesIterator>
{

    private ListIterator<String> branchNamesIterator = Collections.<String>emptyList().listIterator();
    private GithubCommunicator githubCommunicator;
    private SourceControlRepository repository;

    BranchesIterator(List<String> branches, GithubCommunicator githubCommunicator, SourceControlRepository repository)
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

