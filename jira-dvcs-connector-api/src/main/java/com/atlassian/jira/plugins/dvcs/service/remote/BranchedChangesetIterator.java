package com.atlassian.jira.plugins.dvcs.service.remote;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;

/**
 * Iterates all new commits in all branches.
 */
public class BranchedChangesetIterator implements Iterator<Changeset>
{
    private ChangesetIterator changesetIterator;
    private final ListIterator<BranchTip> branchesIterator;
    private final ChangesetCache changesetCache;
    private final Repository repository;
    private final DvcsCommunicator dvcsCommunicator;

    public BranchedChangesetIterator(ChangesetCache changesetCache, DvcsCommunicator dvcsCommunicator,
            					   Repository repository, List<BranchTip> branches)
    {
        this.changesetCache = changesetCache;
        this.dvcsCommunicator = dvcsCommunicator;
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
            BranchTip nextBranch = branchesIterator.next();
            changesetIterator = new ChangesetIterator(dvcsCommunicator, repository, nextBranch, changesetCache);
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
 * Iterates new commits starting on a branchName tip.
 */
class ChangesetIterator implements Iterator<Changeset>
{
    private final DvcsCommunicator dvcsCommunicator;
    private final Repository repository;
    private final Set<String> nextNodes = new HashSet<String>();
    private final String branchName;
    private final ChangesetCache changesetCache;

    public ChangesetIterator(DvcsCommunicator dvcsCommunicator, Repository repository, BranchTip branchTip,
            ChangesetCache changesetCache)
    {
        this.dvcsCommunicator = dvcsCommunicator;
        this.repository = repository;
        this.changesetCache = changesetCache;
        this.branchName = branchTip.getBranchName();
        addNodes(branchTip.getNode());
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
        Changeset currentChangeset = dvcsCommunicator.getDetailChangeset(repository, node);
        
        // mercurial repositories will have branch set for each changeset
        // git repositories don't have branch names set on changesets so we will set our own guessed name
        if (StringUtils.isBlank(currentChangeset.getBranch()))
        {
            currentChangeset.setBranch(branchName);
        }
        
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
