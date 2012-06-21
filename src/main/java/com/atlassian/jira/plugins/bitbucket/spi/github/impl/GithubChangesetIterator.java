package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.PageIterator;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.github.GithubChangesetFactory;

public class GithubChangesetIterator implements Iterator<Changeset>
{
    private ListIterator<Changeset> inPageChangesetsIterator = Collections.<Changeset>emptyList().listIterator();
    private StoppablePageIterator pagesIterator;

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

class StoppablePageIterator implements Iterator<ListIterator<Changeset>>
{
    private final PageIterator<RepositoryCommit> pageIterator;
    private final SourceControlRepository repository;
    private final String branch;
    private boolean stoped = false;


    StoppablePageIterator(PageIterator<RepositoryCommit> pageIterator, SourceControlRepository repository, String branch)
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

        for (RepositoryCommit repositoryCommit : page)
        {
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

    public void stop() {
        this.stoped = true;
    }
}

class BranchesIterator implements Iterator<StoppablePageIterator>
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
    public StoppablePageIterator next()
    {
        if (!hasNext())
        {
            return null;
        }

        String branch = branchNamesIterator.next();
        return new StoppablePageIterator(githubCommunicator.getPageIterator(repository, branch),
                                         repository,
                                         branch);
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}

