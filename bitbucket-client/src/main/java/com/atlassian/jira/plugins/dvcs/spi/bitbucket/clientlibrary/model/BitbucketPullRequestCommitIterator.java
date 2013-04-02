package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

//TODO failure recoveries ... after merge with default
public class BitbucketPullRequestCommitIterator implements Iterator<BitbucketPullRequestCommit>,
        Iterable<BitbucketPullRequestCommit>
{

    // configs
	private final int requestLimit = 30;
    private BitbucketPullRequestCommitEnvelope currentPage = null;

    // services
    private final RemoteRequestor requestor;
    private final String urlIncludingApi;

    /**
     * @param urlIncludingApi
     *            i.e.
     *            /api/2.0/repositories/erik/bitbucket/pullrequests/3/commits
     */
    public BitbucketPullRequestCommitIterator(RemoteRequestor requestor, String urlIncludingApi)
    {
        this.requestor = requestor;
        this.urlIncludingApi = urlIncludingApi + "?pagelen=" + requestLimit;
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
        String url = currentPage == null ? urlIncludingApi : currentPage.getNext();
        // maybe we're just at the end
        if (StringUtils.isBlank(url))
        {
            return;
        }

        requestor.get(url, null, createResponseCallback());
    }

    @Override
    public BitbucketPullRequestCommit next()
    {
        if (currentPage.getValues().isEmpty())
        {
            readPage();
        }

        if (currentPage.getValues().isEmpty())
        {
            throw new NoSuchElementException();
        }

        BitbucketPullRequestCommit currentItem = currentPage.getValues().remove(0);

        //
        // possibly read a next page
        //
        if (currentPage.getValues().isEmpty())
        {
            readPage();
        }

        return currentItem;
    }

    @Override
    public Iterator<BitbucketPullRequestCommit> iterator()
    {
        return this;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("This is unsupported.");
    }

    // ------------------------------------------------------------------------------
    // ------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------
    // ------------------------------------------------------------------------------

    private ResponseCallback<BitbucketPullRequestCommitEnvelope> createResponseCallback()
    {
        return new ResponseCallback<BitbucketPullRequestCommitEnvelope>()
        {
            @Override
            public BitbucketPullRequestCommitEnvelope onResponse(RemoteResponse response)
            {

                BitbucketPullRequestCommitEnvelope remote = ClientUtils.fromJson(response.getResponse(),
                        new TypeToken<BitbucketPullRequestCommitEnvelope>()
                        {
                        }.getType());

                BitbucketPullRequestCommitIterator.this.currentPage = remote;
                return remote;
            }
        };
    }

}
