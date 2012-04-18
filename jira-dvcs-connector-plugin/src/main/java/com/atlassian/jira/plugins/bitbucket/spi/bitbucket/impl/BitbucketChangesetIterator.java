package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An iterator that will load pages of changesets from the remote repository in pages transparently.
 */
public class BitbucketChangesetIterator implements Iterator<Changeset>
{
    public static final int PAGE_SIZE = 15;
    private Iterator<Changeset> currentPage = null;
    private Changeset followingChangset = null; // next changeset after current page
    private final SourceControlRepository repository;
    private final BitbucketCommunicator bitbucketCommunicator;
    private final Date lastCommitDate;

    public BitbucketChangesetIterator(BitbucketCommunicator bitbucketCommunicator, SourceControlRepository repository, Date lastCommitDate)
    {
        this.bitbucketCommunicator = bitbucketCommunicator;
        this.repository = repository;
        this.lastCommitDate = lastCommitDate;
    }

    public boolean hasNext()
    {
        boolean pageHasMoreChangesets = getCurrentPage().hasNext();
        if (!pageHasMoreChangesets && followingChangset != null)
        {
            currentPage = readPage(followingChangset.getNode());
            pageHasMoreChangesets = getCurrentPage().hasNext();
        }

        return pageHasMoreChangesets;
    }

    public Changeset next()
    {
        // we have to call hasNext() here as that will retrieve additional
        // changesets from bitbucket if required
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }
        return getCurrentPage().next();
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    private Iterator<Changeset> getCurrentPage()
    {
        if (currentPage == null)
        {
            currentPage = readPage(null);
        }
        return currentPage;
    }

    private Iterator<Changeset> readPage(String startNode)
    {
        // read PAGE_SIZE + 1 changesets. Last changeset will be used as starting node 
        // for next page (last changeset is actually returned as first in the list)
        List<Changeset> changesets = bitbucketCommunicator.getChangesets(repository, startNode, PAGE_SIZE + 1, lastCommitDate);

        followingChangset = null;
        if (changesets.size() > PAGE_SIZE)
        {
            followingChangset = changesets.remove(0);
        }
        // get the changesets in the correct order (TODO this is probably not required,
        // we sort the changesets before displaying anyway)
        Collections.reverse(changesets);
        return changesets.iterator();

    }
}
