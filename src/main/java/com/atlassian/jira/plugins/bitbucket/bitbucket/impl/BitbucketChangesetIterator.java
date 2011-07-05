package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketAuthentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangeset;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketException;
import com.atlassian.jira.plugins.bitbucket.connection.BitbucketConnection;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An iterator that will load pages of changesets from the remote repository in pages transparently.
 */
public class BitbucketChangesetIterator implements Iterator<BitbucketChangeset>
{
    public static final int PAGE_SIZE = 15;
    private Iterator<BitbucketChangeset> currentPage = null;
    private Integer currentRevision = null;
    private final BitbucketAuthentication auth;
    private final BitbucketConnection bitbucketConnection;
    private final String owner;
    private final String slug;

    public BitbucketChangesetIterator(BitbucketConnection bitbucketConnection, BitbucketAuthentication auth, String owner, String slug)
    {
        this.bitbucketConnection = bitbucketConnection;
        this.auth = auth;
        this.owner = owner;
        this.slug = slug;
    }

    public boolean hasNext()
    {
        if (currentRevision != null && currentRevision == 0)
            return false;

        boolean hasNext = getCurrentPage().hasNext();
        if (!hasNext && currentRevision > 0)
        {
            currentPage = null;
            hasNext = getCurrentPage().hasNext();
        }
        return hasNext;
    }

    public BitbucketChangeset next()
    {
        try
        {
            return getCurrentPage().next();
        }
        catch (NoSuchElementException e)
        {
            // try and load another page if we aren't back to the first revision
            if (currentRevision > 0)
            {
                currentPage = null;
                return getCurrentPage().next();
            }
            else
            {
                throw e;
            }
        }
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    public Iterator<BitbucketChangeset> getCurrentPage()
    {
        if (currentPage == null)
        {
            int limit = currentRevision != null ? Math.min(PAGE_SIZE, currentRevision) : PAGE_SIZE;

            List<BitbucketChangeset> changesets = new ArrayList<BitbucketChangeset>();

            try
            {
                JSONObject page;
                if (currentRevision != null)
                    page = new JSONObject(bitbucketConnection.getChangesets(auth, owner, slug, currentRevision, limit));
                else
                    page = new JSONObject(bitbucketConnection.getChangesets(auth, owner, slug, limit));


                currentRevision = currentRevision == null ? (page.getInt("count") - 1) : currentRevision;

                JSONArray list = page.getJSONArray("changesets");
                for (int i = 0; i < list.length(); i++)
                    changesets.add(BitbucketChangesetFactory.parse(owner, slug, list.getJSONObject(i)));
            }
            catch (JSONException e)
            {
                throw new BitbucketException("could not parse json object", e);
            }

            currentRevision = currentRevision - PAGE_SIZE;
            currentPage = changesets.iterator();

        }
        return currentPage;
    }
}
