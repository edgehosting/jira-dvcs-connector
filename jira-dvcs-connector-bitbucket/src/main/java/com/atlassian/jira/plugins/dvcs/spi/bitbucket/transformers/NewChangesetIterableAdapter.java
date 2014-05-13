package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;

import java.util.Iterator;

public class NewChangesetIterableAdapter implements Iterable<Changeset>, Iterator<Changeset>
{
    private final Iterator<BitbucketNewChangeset> bitbucketChangesetIterator;
    private final int repositoryId;

    public NewChangesetIterableAdapter(Repository repository, Iterable<BitbucketNewChangeset> bitbucketChangesetIterable)
    {
        this.bitbucketChangesetIterator = bitbucketChangesetIterable.iterator();
        this.repositoryId = repository.getId();
    }

    @Override
    public boolean hasNext()
    {
        return bitbucketChangesetIterator.hasNext();
    }

    @Override
    public Changeset next()
    {
        return ChangesetTransformer.fromBitbucketNewChangeset(repositoryId, bitbucketChangesetIterator.next());
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("Remove operation not supported.");
    }

    @Override
    public Iterator<Changeset> iterator()
    {
        return this;
    }
}
