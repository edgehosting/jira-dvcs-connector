package com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;

import java.util.Iterator;

public class ChangesetIterableAdapter implements Iterable<Changeset>, Iterator<Changeset>
{
    private final Iterator<BitbucketChangeset> bitbucketChangesetIterator;
    private final int repositoryId;

    public ChangesetIterableAdapter(Repository repository, Iterable<BitbucketChangeset> bitbucketChangesetIterable)
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
        return ChangesetTransformer.fromBitbucketChangeset(repositoryId, bitbucketChangesetIterator.next());
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
