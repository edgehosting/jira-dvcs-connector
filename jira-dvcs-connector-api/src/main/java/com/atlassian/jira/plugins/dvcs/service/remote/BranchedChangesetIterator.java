package com.atlassian.jira.plugins.dvcs.service.remote;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;

import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.util.Retryer;

import javax.annotation.Nullable;

/**
 * Iterates all new commits in all branches.
 */
public class BranchedChangesetIterator implements Iterator<Changeset>
{
    private ChangesetIterator changesetIterator;
    private final ListIterator<Branch> branchesIterator;
    private final ChangesetCache changesetCache;
    private final Repository repository;
    private final DvcsCommunicator dvcsCommunicator;


    public BranchedChangesetIterator(ChangesetCache changesetCache, DvcsCommunicator dvcsCommunicator,
            					   Repository repository, List<Branch> branches)
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
            Branch nextBranch = branchesIterator.next();
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
    private static final Logger log = LoggerFactory.getLogger(ChangesetIterator.class);

    public ChangesetIterator(DvcsCommunicator dvcsCommunicator, Repository repository, Branch branch,
            ChangesetCache changesetCache)
    {
        this.dvcsCommunicator = dvcsCommunicator;
        this.repository = repository;
        this.changesetCache = changesetCache;
        this.branchName = branch.getName();

        addNodes(Iterables.transform(branch.getHeads(), new Function<BranchHead, String>()
        {
            @Override
            public String apply(@Nullable final BranchHead input)
            {
                return input.getHead();
            }
        }));
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

        try
        {
            Changeset currentChangeset = getDetailedChangeset(node);
            // mercurial repositories will have branch set for each changeset
            // git repositories don't have branch names set on changesets so we will set our own guessed name
            if (StringUtils.isBlank(currentChangeset.getBranch()))
            {
                currentChangeset.setBranch(branchName);
            }
            
            List<String> parents = currentChangeset.getParents();
            addNodes(parents);
            return currentChangeset;
        } catch (Exception e)
        {
            log.warn("Error obtaining detailed changeset for " + repository.getRepositoryUrl() + ", node=" + node, e);
            return null;
        }
    }

    /**
     * Get Detailed Changeset and retry
     * 
     * @param node
     * @return
     */
    private Changeset getDetailedChangeset(final String node)
    {
        // TODO Retrying is now done in bitbucket-client (github doesn't seem to need it). 
        // I think we don't need to retry here anymore
        return new Retryer<Changeset>().retry(new Callable<Changeset>()
        {
            @Override
            public Changeset call() throws Exception
            {
                return dvcsCommunicator.getChangeset(repository, node);
            }
        });
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
    
    private void addNodes(Iterable<String> nodes)
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
