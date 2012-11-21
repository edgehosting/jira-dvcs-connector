package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
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
    public static final int DEFAULT_CHANGESETS_LIMIT = 5;
    private static final int DIFFSTAT_NO_LIMIT = -1;

    private final RemoteRequestor requestor;

    public ChangesetRemoteRestpoint(RemoteRequestor remoteRequestor)
    {
        this.requestor = remoteRequestor;
    }

    public BitbucketChangeset getChangeset(String owner, String slug, String node)
    {
        String getChangesetUrl = String.format("/repositories/%s/%s/changesets/%s", owner, slug, node);

        return requestor.get(getChangesetUrl, null, new ResponseCallback<BitbucketChangeset>()
        {

            @Override
            public BitbucketChangeset onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), BitbucketChangeset.class);
            }

        });

    }

    public List<BitbucketChangesetWithDiffstat> getChangesetDiffStat(String owner, String slug, String node)
    {
        return getChangesetDiffStat(owner, slug, node, DIFFSTAT_NO_LIMIT);
    }

    public List<BitbucketChangesetWithDiffstat> getChangesetDiffStat(String owner, String slug, String node, int limit)
    {
        String getChangesetDiffStatUrl = String.format("/repositories/%s/%s/changesets/%s/diffstat", owner, slug, node);

        Map<String, String> parameters = null;
        if (limit != DIFFSTAT_NO_LIMIT)
        {
            parameters = Collections.singletonMap("limit", "" + limit);
        }

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

    public Iterable<BitbucketChangeset> getChangesets(final String owner, final String slug)
    {
        return getChangesets(owner, slug, null);
    }

    public Iterable<BitbucketChangeset> getChangesets(String owner, String slug, String lastChangesetNode)
    {
        return getChangesets(owner, slug, lastChangesetNode, DEFAULT_CHANGESETS_LIMIT);
    }

    public Iterable<BitbucketChangeset> getChangesets(final String owner, final String slug,
            final String lastChangesetNode, final int changesetsLimit)
    {
        return new Iterable<BitbucketChangeset>()
        {

            @Override
            public Iterator<BitbucketChangeset> iterator()
            {
                return new BitbucketLastNodeChangesetIterator(owner, slug, lastChangesetNode, changesetsLimit);
            }
        };
    }

    private List<BitbucketChangeset> getChangesetsInternal(String owner, String slug, String startNode, int limit)
    {
        String getChangesetsWithPageAndLimitUrl = String.format("/repositories/%s/%s/changesets", owner, slug);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("start", startNode);
        parameters.put("limit", "" + limit);

        try
        {
            return requestor.get(getChangesetsWithPageAndLimitUrl, parameters,
                    new ResponseCallback<List<BitbucketChangeset>>()
                    {
                        @Override
                        public List<BitbucketChangeset> onResponse(RemoteResponse response)
                        {
                            BitbucketChangesetEnvelope bitbucketChangesetEnvelope = ClientUtils.fromJson(response.getResponse(), BitbucketChangesetEnvelope.class);
                            return bitbucketChangesetEnvelope.getChangesets();
                        }
                    });

        } catch (BitbucketRequestException.NotFound_404 notfound)
        {
            // bitbucket return 404 if there is no commit in repo
            return new ArrayList<BitbucketChangeset>();
        }

    }

    private LinkedList<BitbucketChangeset> getChangesetsInternalReversed(String owner, String slug, String startNode, int limit)
    {
        List<BitbucketChangeset> changesets = getChangesetsInternal(owner, slug, startNode, limit);
        Collections.reverse(changesets);
        return new LinkedList<BitbucketChangeset>(changesets);
    }

    private final class BitbucketLastNodeChangesetIterator implements Iterator<BitbucketChangeset>
    {//TODO change to getNode() ???
        private final String owner;
        private final String slug;
        private final String lastChangesetNode;
        private final int changesetsLimit;

        private final LinkedList<BitbucketChangeset> changesetQueue;

        private BitbucketLastNodeChangesetIterator(String owner, String slug, final String lastChangesetNode,
                final int changesetsLimit)
        {           
            this.owner = owner;
            this.slug = slug;
            this.lastChangesetNode = lastChangesetNode;
            this.changesetsLimit = changesetsLimit;

            changesetQueue = getChangesetsInternalReversed(owner, slug, "tip", changesetsLimit);
        }

        @Override
        public boolean hasNext() // hasNext() cannot modify the state of queue, otherwise multiple calls would cause side-effect !!!
        {           
            return !(changesetQueue.isEmpty() || // the queue is empty when we iterated over all changesets or repository is empty
                   changesetQueue.peek().getRawNode().equals(lastChangesetNode));
        }//TODO node vs rawNode => preco vobec existuje node ak vsetko sa da robit cez rawNode??? dat prec aby nevznikli chyby

        @Override
        public BitbucketChangeset next()
        {
            if (hasNext())
            {
                if (changesetQueue.size() == 1) // here we need to read another page
                {
                    LinkedList<BitbucketChangeset> changesetsInCurrentPage = getChangesetsInternalReversed(owner, slug, changesetQueue.peek().getNode(), changesetsLimit + 1);

                    changesetsInCurrentPage.poll(); // remove first if exists as the first one was already processed in the previous page as last element

                    changesetQueue.addAll(changesetsInCurrentPage);
                }

                return changesetQueue.poll();
            }
            else
            {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("Remove operation not supported.");
        }
    }
}
