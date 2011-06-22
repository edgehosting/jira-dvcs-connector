package com.atlassian.jira.plugins.bitbucket.bitbucket.remote;

import com.atlassian.jira.plugins.bitbucket.bitbucket.*;
import com.atlassian.jira.plugins.bitbucket.bitbucket.connection.BitbucketConnection;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public class RemoteBitbucket implements Bitbucket
{
    public static final int PAGE_SIZE = 15;

    private final BitbucketConnection bitbucketConnection;

    public RemoteBitbucket(BitbucketConnection bitbucketConnection)
    {
        this.bitbucketConnection = bitbucketConnection;
    }

    public BitbucketUser getUser(String username)
    {
        try
        {
            return RemoteBitbucketUser.parse(new JSONObject(bitbucketConnection.getUser(username)));
        }
        catch (JSONException e)
        {
            throw new BitbucketException("could not parse json object", e);
        }
    }

    public BitbucketRepository getRepository(BitbucketAuthentication auth, String owner, String slug)
    {
        try
        {
            return RemoteBitbucketRepository.parse(new JSONObject(bitbucketConnection.getRepository(auth, owner, slug)));
        }
        catch (JSONException e)
        {
            throw new BitbucketException("could not parse json object", e);
        }
    }

    public BitbucketChangeset getChangeset(BitbucketAuthentication auth, String owner, String slug, String id)
    {
        try
        {
            return RemoteBitbucketChangeset.parse(new JSONObject(bitbucketConnection.getChangeset(auth, owner, slug, id)));
        }
        catch (JSONException e)
        {
            throw new BitbucketException("could not parse json result", e);
        }
    }

    public List<BitbucketChangeset> getChangesets(BitbucketAuthentication auth, String owner, String slug)
    {
        List<BitbucketChangeset> changesets = new ArrayList<BitbucketChangeset>();

        int currentRevision = -1;
        do
        {
            String start = currentRevision > 0 ? String.valueOf(currentRevision) : "tip";
            int limit = currentRevision > 0 ? Math.min(RemoteBitbucket.PAGE_SIZE, currentRevision) : RemoteBitbucket.PAGE_SIZE;

            try
            {
                JSONObject page = new JSONObject(bitbucketConnection.getChangesets(auth, owner, slug, start, limit));
                currentRevision = currentRevision < 0 ? (page.getInt("count") - 1) : currentRevision;

                JSONArray list = page.getJSONArray("changesets");
                for (int i = 0; i < list.length(); i++)
                    changesets.add(RemoteBitbucketChangeset.parse(list.getJSONObject(i)));
            }
            catch (JSONException e)
            {
                throw new BitbucketException("could not parse json object", e);
            }

            currentRevision = currentRevision - RemoteBitbucket.PAGE_SIZE;
        } while (currentRevision > 0);

        Collections.sort(changesets, new Comparator<BitbucketChangeset>()
        {
            public int compare(BitbucketChangeset a, BitbucketChangeset b)
            {
                return a.getRevision() - b.getRevision();
            }
        });

        return changesets;
    }


}
