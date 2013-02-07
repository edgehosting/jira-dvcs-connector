package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

//TODO failure recoveries ... after merge with default
public class BitbucketPullRequestActivityIterator implements Iterator<BitbucketPullRequestActivityInfo>,
        Iterable<BitbucketPullRequestActivityInfo>
{
    // configs
    private int requestLimit = 30;
    private final Date upToDate;
    private boolean wasDateOver = false;
    private final String forUser;
    private final String forRepoSlug;
    private BitbucketPullRequestBaseActivityEnvelope currentPage = null;

    // services
    private final RemoteRequestor requestor;

    public BitbucketPullRequestActivityIterator(RemoteRequestor requestor, Date upToDate, String forRepoSlug,
            String forUser)
    {
        this.requestor = requestor;
        this.upToDate = upToDate != null ? upToDate : new Date(0);
        this.forRepoSlug = forRepoSlug;
        this.forUser = forUser;
    }

    @Override
    public boolean hasNext()
    {
        // not initialized
        if (currentPage == null)
        {
            readPage();
        }

    	if (wasDateOver)
    	{
    		return false;
    	}
        
        return !currentPage.getValues().isEmpty();
    }

    private void readPage()
    {
        if (wasDateOver)
        {
            this.currentPage.getValues().clear();
        } else
        {
        	String url = currentPage == null ? createUrl() : currentPage.getNext();
        	if (url == null || url.trim().isEmpty())
        	{
        		return;
        	}
            requestor.get(url, createRequestParams(), createResponseCallback());
        }
    }

    @Override
    public BitbucketPullRequestActivityInfo next()
    {
    	if (!hasNext())
    	{
    		throw new NoSuchElementException();
    	}
    	
        BitbucketPullRequestActivityInfo currentItem = currentPage.getValues().remove(0);

        //
        // possibly read a next page
        //
        if (currentPage.getValues().isEmpty())
        {
            readPage();
        }
        
        if (!currentPage.getValues().isEmpty())
        {
        	BitbucketPullRequestActivityInfo nextItem = currentPage.getValues().get(0);
        	checkDate(nextItem.getActivity());
        }
        
        return currentItem;
    }

    @Override
    public Iterator<BitbucketPullRequestActivityInfo> iterator()
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

    private void checkDate(BitbucketPullRequestBaseActivity activity)
    {
    	Date date = activity.extractDate();
    	if (date!=null && !date.after(upToDate))
    	{
            wasDateOver = true;
    	}
    }
    
    private HashMap<String, String> createRequestParams()
    {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("pagelen", requestLimit + "");
        return params;
    }

    private ResponseCallback<BitbucketPullRequestBaseActivityEnvelope> createResponseCallback()
    {
        return new ResponseCallback<BitbucketPullRequestBaseActivityEnvelope>()
        {
            @Override
            public BitbucketPullRequestBaseActivityEnvelope onResponse(RemoteResponse response)
            {
                BitbucketPullRequestBaseActivityEnvelope remote = 
                        ClientUtils.fromJson(
                                                                  response.getResponse(), 
                                                                  new TypeToken<BitbucketPullRequestBaseActivityEnvelope>(){}.getType()
                                                              );

                if (remote != null && remote.getValues() != null && !remote.getValues().isEmpty())
                {
                	checkDate(remote.getValues().get(0).getActivity());
                }

                BitbucketPullRequestActivityIterator.this.currentPage = remote;
                return remote;
            }

        };
    }

    private String createUrl()
    {
        return String.format("/repositories/%s/%s/pullrequests/activity", forUser, forRepoSlug);
    }
}
