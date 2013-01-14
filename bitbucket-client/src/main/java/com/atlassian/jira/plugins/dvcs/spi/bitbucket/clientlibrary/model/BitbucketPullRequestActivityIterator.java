package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

//TODO failure recoveries ... after merge with default
public class BitbucketPullRequestActivityIterator implements Iterator<BitbucketPullRequestBaseActivity>,
        Iterable<BitbucketPullRequestBaseActivity>
{

    // configs
    private int start = 0;
    private int requestLimit = 30;
    private final Date upToDate;
    private boolean wasDateOver = false;
    private final String forUser;
    private final String forRepoSlug;
    private List<BitbucketPullRequestBaseActivity> currentFrame = null;

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
        if (currentFrame == null)
        {
            readFrame();
        }

        return !currentFrame.isEmpty();
    }

    private void readFrame()
    {
        if (wasDateOver)
        {

            this.currentFrame = new ArrayList<BitbucketPullRequestBaseActivity>();

        } else
        {

            requestor.get(createUrl(), createRequestParams(), createResponseCallback());
            start++;

        }
    }

    @Override
    public BitbucketPullRequestBaseActivity next()
    {
        if (currentFrame.isEmpty())
        {
            readFrame();
        }

        if (currentFrame.isEmpty())
        {
            throw new NoSuchElementException();
        }

        BitbucketPullRequestBaseActivity currentItem = currentFrame.remove(0);

        //
        // possibly read a next frame
        //
        if (currentFrame.isEmpty())
        {
            readFrame();
        }

        return currentItem;
    }

    @Override
    public Iterator<BitbucketPullRequestBaseActivity> iterator()
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

    private HashMap<String, String> createRequestParams()
    {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("start", start + "");
        params.put("limit", requestLimit + "");
        return params;
    }

    private ResponseCallback<List<BitbucketPullRequestBaseActivity>> createResponseCallback()
    {
        return new ResponseCallback<List<BitbucketPullRequestBaseActivity>>()
        {
            @Override
            public List<BitbucketPullRequestBaseActivity> onResponse(RemoteResponse response)
            {
                List<BitbucketPullRequestBaseActivity> remote = 
                        ClientUtils.fromJsonWithDeserializers(
                                                                  response.getResponse(), 
                                                                  new TypeToken<List<BitbucketPullRequestBaseActivity>>(){}.getType(),
                                                                  BitbucketPullRequestActivityDeserializer.asMap()
                                                              );
                
                List<BitbucketPullRequestBaseActivity> ret = new ArrayList<BitbucketPullRequestBaseActivity>();

                for (BitbucketPullRequestBaseActivity remoteActivity : remote)
                {
                    if (remoteActivity.getUpdatedOn().after(upToDate))
                    {
                        ret.add(remoteActivity);
                    } else
                    {
                        wasDateOver = true;
                    }
                }
                BitbucketPullRequestActivityIterator.this.currentFrame = ret;
                return ret;
            }
        };
    }

    private String createUrl()
    {
        return String.format("/repositories/%s/%s/pullrequests/activity", forUser, forRepoSlug);
    }

}
