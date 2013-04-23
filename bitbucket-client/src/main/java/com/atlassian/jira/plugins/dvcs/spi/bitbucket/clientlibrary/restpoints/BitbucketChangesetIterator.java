package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.Iterator;
import java.util.List;
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
    private final List<String> excludeNodes;
    private BitbucketChangesetPage currentPage = null;
    
    // services
    private final RemoteRequestor requestor;

    public BitbucketChangesetIterator(RemoteRequestor requestor, String owner, String slug, List<String> excludeNodes, int pageLength)
    {
        this.requestor = requestor;
        this.owner = owner;
        this.slug = slug;
        this.excludeNodes = excludeNodes;
        this.pageLength = pageLength;
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
        String url = currentPage == null ? createUrl() : currentPage.getNext();
        if (StringUtils.isBlank(url))
        {
            return;
        }
        currentPage = requestor.get(url, null, createResponseCallback());
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

        return currentChangeset;
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
        StringBuilder url = new StringBuilder(String.format("/api/2.0/repositories/%s/%s/walk/?pagelen=%s", owner, slug, pageLength));
        
        if (excludeNodes != null)
        {
            for (String branchTip : excludeNodes)
            {
                url.append("&excludes=").append(branchTip);
            }
        }
        
        return url.toString();
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
