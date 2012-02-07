package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;

import java.util.*;

public class GithubChangesetIterator implements Iterator<Changeset>
{
    private ListIterator<Changeset> inPageChangesetsIterator = Collections.<Changeset>emptyList().listIterator();
    private PagesIterator pagesIterator;

    private BranchesIterator branchesIterator;
    private GithubRepositoryManager githubRepositoryManager;

    public GithubChangesetIterator(GithubRepositoryManager repositoryManager, GithubCommunicator githubCommunicator, SourceControlRepository repository, List<String> branches)
    {
        this.githubRepositoryManager = repositoryManager;
        branchesIterator = new BranchesIterator(branches, githubCommunicator, repository);
        pagesIterator = branchesIterator.next();
    }

    public boolean hasNext()
    {
        return inPageChangesetsIterator.hasNext() || (pagesIterator != null && pagesIterator.hasNext()) || branchesIterator.hasNext() ;
    }

    int i=0;

    public Changeset next()
    {
        if (inPageChangesetsIterator.hasNext())
        {
            i++;
            final Changeset nextChangeset = inPageChangesetsIterator.next();

            if (githubRepositoryManager.wasChangesetAlreadySynchronized(nextChangeset.getNode()))
            {
                inPageChangesetsIterator = Collections.<Changeset>emptyList().listIterator();
                pagesIterator.stop();
            }
            return nextChangeset;
        } else if (pagesIterator.hasNext())
        {
            inPageChangesetsIterator = pagesIterator.next();
            return next();
        } else if (branchesIterator.hasNext())
        {
            i = 0;
            pagesIterator = branchesIterator.next();
            return next();
        }

        throw new NoSuchElementException();
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

