package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

/**
 * ChangesetRemoteRestpoint
 * 
 * @author Martin Skurla mskurla@atlassian.com
 */
public class ChangesetRemoteRestpoint
{
    private static final ResponseCallback<BitbucketChangesetPage> BITBUCKET_CHANGESETS_PAGE_RESPONSE = new ResponseCallback<BitbucketChangesetPage>()
    {

        @Override
        public BitbucketChangesetPage onResponse(RemoteResponse response)
        {
            return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketChangesetPage>()
            {
            }.getType());
        }

    };
    private final RemoteRequestor requestor;

    public ChangesetRemoteRestpoint(RemoteRequestor remoteRequestor)
    {
        this.requestor = remoteRequestor;
    }

    public BitbucketChangeset getChangeset(String owner, String slug, String node)
    {
        String getChangesetUrl = URLPathFormatter.format("/repositories/%s/%s/changesets/%s", owner, slug, node);

        return requestor.get(getChangesetUrl, null, new ResponseCallback<BitbucketChangeset>()
        {

            @Override
            public BitbucketChangeset onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), BitbucketChangeset.class);
            }
        });
    }

    // "/api/1.0/repositories/erik/bitbucket/changesets/4a233e7b8596e5b17dd672f063e40f7c544c2c81"
    public BitbucketChangeset getChangeset(String urlIncludingApi)
    {
        return requestor.get(URLPathFormatter.format(urlIncludingApi), null, new ResponseCallback<BitbucketChangeset>()
        {

            @Override
            public BitbucketChangeset onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), BitbucketChangeset.class);
            }

        });

    }

    public List<BitbucketChangesetWithDiffstat> getChangesetDiffStat(String owner, String slug, String node, int limit)
    {
        String getChangesetDiffStatUrl = URLPathFormatter.format("/repositories/%s/%s/changesets/%s/diffstat", owner, slug, node);

        Map<String, String> parameters = null;
        // Requesting one more stat than limit to find out whether there are more stats
        parameters = Collections.singletonMap("limit", "" + (limit + 1));

        return requestor.get(getChangesetDiffStatUrl, parameters,
                new ResponseCallback<List<BitbucketChangesetWithDiffstat>>()
                {
                    @Override
                    public List<BitbucketChangesetWithDiffstat> onResponse(RemoteResponse response)
                    {
                        return ClientUtils.fromJson(response.getResponse(),
                                new TypeToken<List<BitbucketChangesetWithDiffstat>>()
                                {
                                }.getType());
                    }
                });
    }

    public Iterable<BitbucketNewChangeset> getChangesets(final String owner, final String slug, final List<String> includeNodes,
                                                         final List<String> excludeNodes, final Map<String,String> changesetBranch, final int changesetsLimit)
    {
        return new Iterable<BitbucketNewChangeset>()
        {
            @Override
            public Iterator<BitbucketNewChangeset> iterator()
            {
                return new BitbucketChangesetIterator(requestor, owner, slug, includeNodes, excludeNodes, changesetBranch, changesetsLimit);
            }
        };
    }
    @Deprecated // TODO<jhocman> remove
    public BitbucketChangesetPage getChangesetsForFirstPage(String owner, String slug, List<String> includeNodes,
            List<String> excludeNodes, Map<String, String> changesetBranch, int changesetsLimit)
    {
        String url = String.format("/api/2.0/repositories/%s/%s/commits/?pagelen=%s", owner, slug, changesetsLimit);
        Map<String, Object> parameters = new HashMap<String, Object>();
        if (includeNodes != null)
        {
            parameters.put("include", new ArrayList<String>(includeNodes));
        }

        if (excludeNodes != null)
        {
            parameters.put("exclude", new ArrayList<String>(excludeNodes));
        }

        return requestor.post(url, parameters, BITBUCKET_CHANGESETS_PAGE_RESPONSE);
    }

    public BitbucketChangesetPage getChangesetsForPage(final int page, final String owner, final String slug,
            final int changesetsLimit, final List<String> includeNodes, final List<String> excludeNodes)
    {
        String url = String.format("/api/2.0/repositories/%s/%s/commits/?pagelen=%s&page=%s", owner, slug, changesetsLimit, page);
        Map<String, Object> parameters = new HashMap<String, Object>();

        if (includeNodes != null)
        {
            parameters.put("include", new ArrayList<String>(includeNodes));
        }

        if (excludeNodes != null)
        {
            parameters.put("exclude", new ArrayList<String>(excludeNodes));
        }
        return requestor.post(url, null, BITBUCKET_CHANGESETS_PAGE_RESPONSE);
    }

}
