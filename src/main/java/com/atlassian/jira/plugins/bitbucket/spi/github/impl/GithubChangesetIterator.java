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
    private GithubCommunicator githubCommunicator;
    private SourceControlRepository repository;

    private int currentPageNumber = 0;   // github gives us pages indexed from 1 (zero is one before)
    private ListIterator<Changeset> inPageIterator = Collections.<Changeset>emptyList().listIterator();

    public GithubChangesetIterator(GithubCommunicator githubCommunicator, SourceControlRepository repository)
    {
        this.githubCommunicator = githubCommunicator;
        this.repository = repository;
    }

    public boolean hasNext()
    {
        if (inPageIterator.hasNext())
        {
            return true;
        }

        currentPageNumber++;
        // todo: do iteration through all branches
        List<Changeset> changesets = githubCommunicator.getChangesets(repository, "master", currentPageNumber);
        if (changesets == null || changesets.isEmpty())
        {
            return false;
        }
        inPageIterator = changesets.listIterator();
        return hasNext();
    }

    public Changeset next()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }

        return inPageIterator.next();
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
