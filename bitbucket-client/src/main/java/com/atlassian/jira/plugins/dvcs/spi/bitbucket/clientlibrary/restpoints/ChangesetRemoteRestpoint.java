package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
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


    public List<BitbucketChangesetWithDiffstat> getChangesetDiffStat(String owner, String slug, String node, int limit)
    {
        String getChangesetDiffStatUrl = URLPathFormatter.format("/repositories/%s/%s/changesets/%s/diffstat", owner, slug, node);

        Map<String, String> parameters = null;
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

}
