package com.atlassian.jira.plugins.dvcs.spi.github;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.commons.collections.CollectionUtils;
import org.eclipse.egit.github.core.RepositoryBranch;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.google.common.collect.Lists;

public class GithubChangesetIterator implements Iterator<Changeset>
{
    private ChangesetIterator changesetIterator;

    private final BranchesIterator branchesIterator;

    private final Repository repository;

    private Changeset nextChangeset;
    private final ChangesetCache changesetCache;

    public GithubChangesetIterator(final ChangesetCache changesetCache, final GithubCommunicator githubCommunicator,
            					   final Repository repository, final List<RepositoryBranch> branches, final Date lastCommitDate)
    {
        this.changesetCache = changesetCache;
        this.repository = repository;

        branchesIterator = new BranchesIterator(branches, githubCommunicator, repository);
        if (branchesIterator.hasNext())
        {
            changesetIterator = branchesIterator.next();
        }
    }

    @Override
    public boolean hasNext()
    {
        final boolean hasNext = changesetIterator != null
                && (nextChangeset != null || changesetIterator.hasNext() || branchesIterator.hasNext());
        if (hasNext)
        {
            if (nextChangeset == null)
            {
                nextChangeset = internalNext();
            }

            if (shoudStopBranchIteration())
            {
                changesetIterator.stop();
                nextChangeset = null;
                return hasNext();
            }

        }
        return hasNext;
    }

    private boolean shoudStopBranchIteration()
    {
        return changesetCache.isCached(repository.getId(), nextChangeset.getNode());
    }

    private Changeset internalNext()
    {
        if (changesetIterator.hasNext())
        {
            return changesetIterator.next();
        } else if (branchesIterator.hasNext())
        {
            changesetIterator = branchesIterator.next();
            return internalNext();
        }

        throw new NoSuchElementException();

    }

    @Override
    public Changeset next()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException();
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

class BranchesIterator implements Iterator<ChangesetIterator>
{

    private ListIterator<RepositoryBranch> branchesIterator = Collections.<RepositoryBranch> emptyList().listIterator();
    private final GithubCommunicator githubCommunicator;
    private final Repository repository;

    BranchesIterator(List<RepositoryBranch> branches, GithubCommunicator githubCommunicator, Repository repository)
    {
        this.githubCommunicator = githubCommunicator;
        this.repository = repository;
        this.branchesIterator = branches.listIterator();
    }

    @Override
    public boolean hasNext()
    {
        return branchesIterator.hasNext();
    }

    @Override
    public ChangesetIterator next()
    {
        if (!hasNext())
        {
            return null;
        }

        final RepositoryBranch branch = branchesIterator.next();
        return new ChangesetIterator(githubCommunicator, repository, branch);
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}

class ChangesetIterator implements Iterator<Changeset>
{
    private final GithubCommunicator githubCommunicator;
    private final Repository repository;
    private final Stack<String> changesetStack = new Stack<String>();
    private final String branch;
    private List<String> nextChangesets;

    public ChangesetIterator(GithubCommunicator githubCommunicator, Repository repository, RepositoryBranch branch)
    {
        this.githubCommunicator = githubCommunicator;
        this.repository = repository;
        this.branch = branch.getName();
        nextChangesets = Arrays.asList(branch.getCommit().getSha());
    }

    @Override
    public boolean hasNext()
    {
        return CollectionUtils.isNotEmpty(nextChangesets) || !changesetStack.isEmpty();
    }

    @Override
    public Changeset next()
    {
        if (CollectionUtils.isNotEmpty(nextChangesets))
        {
            // we place next changesets on the top of the stack
            for (String parent : Lists.reverse(nextChangesets))
            {
                changesetStack.push(parent);
            }
        }

        if (!changesetStack.isEmpty())
        {
            String node = changesetStack.pop();
            Changeset currentChangeset = githubCommunicator.getDetailChangeset(repository, branch, node);
            nextChangesets = currentChangeset.getParents();
            return currentChangeset;
        }

        throw new NoSuchElementException();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    public void stop()
    {
        // we need to clean next changesets as we don't want to process them
        nextChangesets = null;
    }
}
