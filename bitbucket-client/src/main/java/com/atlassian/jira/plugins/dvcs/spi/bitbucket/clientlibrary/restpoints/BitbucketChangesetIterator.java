package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

public class BitbucketChangesetIterator implements Iterator<BitbucketNewChangeset>
{
    // configuration
    private final int pageLength;
    private final String owner;
    private final String slug;
    private final List<String> includeNodes;
    private final List<String> excludeNodes;
    private BitbucketChangesetPage currentPage = null;
    private final Map<String,String> changesetBranch;

    // services
    private final ChangesetRemoteRestpoint changesetRemoteRestpoint;

    public BitbucketChangesetIterator(ChangesetRemoteRestpoint changesetRemoteRestpoint, String owner, String slug, List<String> includeNodes, List<String> excludeNodes, Map<String,String> changesetBranch, int pageLength, BitbucketChangesetPage currentPage)
    {
        this.changesetRemoteRestpoint = changesetRemoteRestpoint;
        this.owner = owner;
        this.slug = slug;
        this.includeNodes = includeNodes;
        this.excludeNodes = excludeNodes;
        this.pageLength = pageLength;
        this.currentPage = currentPage;
        this.changesetBranch = changesetBranch;
    }

    @Override
    public boolean hasNext()
    {
        // not initialized
        if (currentPage == null)
        {
            readPage();
        }

        return !currentPage.getValues().isEmpty() || currentPage.getNext() != null;
    }

    private void readPage()
    {
        currentPage = changesetRemoteRestpoint.getNextChangesetsPage(owner, slug, includeNodes, excludeNodes, pageLength, currentPage);
    }

            @Override
    public BitbucketNewChangeset next()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }

        if (currentPage.getValues().isEmpty())
        {
            readPage();
        }

        BitbucketNewChangeset currentChangeset = currentPage.getValues().remove(0);

        assignBranch(currentChangeset);

        return currentChangeset;
    }

    // TODO This code is duplicated between here and BitbucketSynchronizeChangesetMessageConsumer
    private void assignBranch(BitbucketNewChangeset changeset)
    {
        String branch = changesetBranch.get(changeset.getHash());
        changeset.setBranch(branch);
        changesetBranch.remove(changeset.getHash());
        for (BitbucketNewChangeset parent : changeset.getParents())
        {
            changesetBranch.put(parent.getHash(), branch);
        }
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("This is unsupported.");
    }
}
