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

public class BitbucketChangesetIterator implements Iterator<BitbucketNewChangeset>, Iterable<BitbucketNewChangeset>
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
    private final RemoteRequestor requestor;

    public BitbucketChangesetIterator(RemoteRequestor requestor, String owner, String slug, List<String> includeNodes, List<String> excludeNodes, Map<String,String> changesetBranch, int pageLength)
    {
        this.requestor = requestor;
        this.owner = owner;
        this.slug = slug;
        this.includeNodes = includeNodes;
        this.excludeNodes = excludeNodes;
        this.pageLength = pageLength;
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
        Map<String, List<String>> parameters = null;
        String url = null;

        if (currentPage == null)
        {
            // this is the first request, first page
            url = createUrl();

            parameters = new HashMap<String, List<String>>();
            if (includeNodes != null)
            {
                parameters.put("include", new ArrayList<String>(includeNodes));
            }
            if (excludeNodes != null)
            {
                parameters.put("exclude", new ArrayList<String>(excludeNodes));
            }
        } else
        {
            url = currentPage.getNext();
        }

        if (StringUtils.isBlank(url))
        {
            return;
        }

        currentPage = requestor.getWithMultipleVals(url, parameters, createResponseCallback());
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

    @Override
    public Iterator<BitbucketNewChangeset> iterator()
    {
        return this;
    }

    private String createUrl()
    {
        return String.format("/api/2.0/repositories/%s/%s/commits/?pagelen=%s", owner, slug, pageLength);
    }

    private ResponseCallback<BitbucketChangesetPage> createResponseCallback()
    {
        return new ResponseCallback<BitbucketChangesetPage>()
        {
            @Override
            public BitbucketChangesetPage onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketChangesetPage>(){}.getType());
            }
        };
    }
}
