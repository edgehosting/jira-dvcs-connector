package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.atlassian.jira.plugins.bitbucket.bitbucket.Authentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketException;
import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.connection.BitbucketConnection;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * An iterator that will load pages of changesets from the remote repository in pages transparently.
 */
public class BitbucketChangesetIterator implements Iterator<Changeset>
{
    public static final int PAGE_SIZE = 15;
    private Iterator<Changeset> currentPage = null;
    private Changeset followingChangset = null; // next changeset after current page
    private final Authentication auth;
    private final BitbucketConnection bitbucketConnection;
	private RepositoryUri uri;

    public BitbucketChangesetIterator(BitbucketConnection bitbucketConnection, Authentication auth, String owner, String slug)
    {
        this.bitbucketConnection = bitbucketConnection;
        this.auth = auth;
        this.uri = new RepositoryUri(owner,slug);
    }

    public boolean hasNext()
    {
    	boolean pageHasMoreChansets = getCurrentPage().hasNext();
        if (!pageHasMoreChansets && followingChangset!=null)
        {
        	currentPage = readPage(followingChangset.getNode());
            pageHasMoreChansets = getCurrentPage().hasNext();
        }
        
        return pageHasMoreChansets;
    }

	public Changeset next()
	{
		// we have to call hasNext() here as that will retrieve additional changesets from bitbucket if required
		if (!hasNext())
		{
			throw new NoSuchElementException();
		}
		return getCurrentPage().next();
	}

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    
    private Iterator<Changeset> getCurrentPage()
    {
    	if (currentPage == null) 
    	{
    		currentPage = readPage(null);
    	}
    	return currentPage;
    }
    
    private Iterator<Changeset> readPage(String startNode)
    {
        List<Changeset> changesets = new ArrayList<Changeset>();

        try
        {
        	// read PAGE_SIZE + 1 changesets. Last changeset will be used as starting node for next page
			String response = bitbucketConnection.getChangesets(auth, uri.getOwner(), uri.getSlug(), startNode, PAGE_SIZE + 1);

			JSONArray list = new JSONObject(response).getJSONArray("changesets");
			followingChangset = null;
            if (list.length()>PAGE_SIZE)
            {
            	followingChangset = BitbucketChangesetFactory.parse(uri.getRepositoryUrl(), list.getJSONObject(0));
            }
            int startIndex = followingChangset==null?0:1;
            for (int i = startIndex; i < Math.min(list.length(), PAGE_SIZE+1); i++)
            {
            	changesets.add(BitbucketChangesetFactory.parse(uri.getRepositoryUrl(), list.getJSONObject(i)));
            }
            // get the changesets in the correct order
            Collections.reverse(changesets);
            return changesets.iterator();
        }
        catch (JSONException e)
        {
            throw new BitbucketException("could not parse json object", e);
        }

    }
}
