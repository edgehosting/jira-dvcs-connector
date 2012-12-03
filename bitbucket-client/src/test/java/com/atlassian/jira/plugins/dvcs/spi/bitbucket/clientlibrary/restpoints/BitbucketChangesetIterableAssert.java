package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;


import java.util.LinkedHashSet;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;

import org.fest.assertions.api.Assertions;
import org.fest.assertions.api.IterableAssert;


class BitbucketChangesetIterableAssert extends IterableAssert<BitbucketChangeset>
{
    private final Set<String> changesetNodes = new LinkedHashSet<String>();
    private int changesetCounter = 0;


    private BitbucketChangesetIterableAssert(Iterable<BitbucketChangeset> changesets)
    {
        super(changesets);

        for (BitbucketChangeset bitbucketChangeset : changesets)
        {
            changesetCounter++;
            changesetNodes.add(bitbucketChangeset.getNode());
        }
    }

    static BitbucketChangesetIterableAssert assertThat(Iterable<BitbucketChangeset> changesets)
    {
        return new BitbucketChangesetIterableAssert(changesets);
    }

    BitbucketChangesetIterableAssert hasNumberOfChangesets(int expectedChangesetsCount)
    {
        Assertions.assertThat(changesetCounter).isEqualTo(expectedChangesetsCount);
        return this;
    }

    BitbucketChangesetIterableAssert hasNumberOfUniqueChangesets(int expectedChangesetsCount)
    {
        Assertions.assertThat(changesetNodes).hasSize(expectedChangesetsCount);
        return this;
    }

    BitbucketChangesetIterableAssert hasChangesetsWithNodesInOrder(String... changesetNodes)
    {
        Assertions.assertThat(changesetNodes).isEqualTo(changesetNodes);
        return this;
    }
}