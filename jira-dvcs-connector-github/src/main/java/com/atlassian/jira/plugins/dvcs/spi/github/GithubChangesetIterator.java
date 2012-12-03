package com.atlassian.jira.plugins.dvcs.spi.github;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.egit.github.core.RepositoryBranch;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;

/**
 * Iterates all new commits in all branches.
 */
public class GithubChangesetIterator implements Iterator<Changeset>
{
    private ChangesetIterator changesetIterator;
    private final ListIterator<RepositoryBranch> branchesIterator;
    private final ChangesetCache changesetCache;
    private final GithubCommunicator githubCommunicator;
    private final Repository repository;

    public GithubChangesetIterator(ChangesetCache changesetCache, GithubCommunicator githubCommunicator,
            					   Repository repository, List<RepositoryBranch> branches)
    {
        this.changesetCache = changesetCache;
        this.githubCommunicator = githubCommunicator;
        this.repository = repository;
        this.branchesIterator = branches.listIterator();
    }

    @Override
    public boolean hasNext()
    {
        if (changesetIterator!=null && changesetIterator.hasNext())
        {
            return true;
        }
        if (branchesIterator.hasNext())
        {
            RepositoryBranch nextBranch = branchesIterator.next();
            changesetIterator = new ChangesetIterator(githubCommunicator, repository, nextBranch, changesetCache);
            return hasNext();
        }
        return false;
    }

    @Override
    public Changeset next()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }

        return changesetIterator.next();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}

/**
 * Iterates new commits starting on a branch tip.
 */
class ChangesetIterator implements Iterator<Changeset>
{
    private final GithubCommunicator githubCommunicator;
    private final Repository repository;
    private final Set<String> nextNodes = new HashSet<String>();
    private final String branch;
    private final ChangesetCache changesetCache;

    public ChangesetIterator(GithubCommunicator githubCommunicator, Repository repository, RepositoryBranch branch,
            ChangesetCache changesetCache)
    {
        this.githubCommunicator = githubCommunicator;
        this.repository = repository;
        this.changesetCache = changesetCache;
        this.branch = branch.getName();
        addNodes(branch.getCommit().getSha());
    }

    @Override
    public boolean hasNext()
    {
        return !nextNodes.isEmpty();
    }

    @Override
    public Changeset next()
    {
        if (nextNodes.isEmpty())
        {
            throw new NoSuchElementException();
        }
        
        Iterator<String> it = nextNodes.iterator();
        String node = it.next();
        it.remove();
        Changeset currentChangeset = githubCommunicator.getDetailChangeset(repository, branch, node);
        
        List<String> parents = currentChangeset.getParents();
        addNodes(parents.toArray(new String[0]));
        return currentChangeset;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
    
    private void addNodes(String... nodes)
    {
        for (String node : nodes)
        {
            if (!changesetCache.isCached(repository.getId(), node))
            {
                nextNodes.add(node);
            }
        }
    }


}
