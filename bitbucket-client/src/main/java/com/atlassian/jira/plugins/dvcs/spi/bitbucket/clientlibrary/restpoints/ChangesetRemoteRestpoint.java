package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;

/**
 * ChangesetRemoteRestpoint
 * 
 * @author Martin Skurla mskurla@atlassian.com
 */
public class ChangesetRemoteRestpoint
{
    private static final int DEFAULT_CHANGESETS_LIMIT = 5;
    private static final int DIFFSTAT_NO_LIMIT = -1;
    
    private final RemoteRequestor requestor;
    

	public ChangesetRemoteRestpoint(RemoteRequestor remoteRequestor)
	{
		this.requestor = remoteRequestor;
	}
    
    
    public BitbucketChangeset getChangeset(String owner, String slug, String node)
    {
        String getChangesetUrl = String.format("/repositories/%s/%s/changesets/%s", owner, slug, node);
        
        RemoteResponse response = requestor.get(getChangesetUrl, null);
        
        return ClientUtils.fromJson(response.getResponse(), BitbucketChangeset.class);
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
        
        RemoteResponse response = requestor.get(getChangesetDiffStatUrl, parameters);
        
        return ClientUtils.fromJson(response.getResponse(),
                                    new TypeToken<List<BitbucketChangesetWithDiffstat>>(){}.getType());
    }
    
    public Iterable<BitbucketChangeset> getChangesets(final String owner, final String slug, final String lastChangesetNode)
    {
        return getChangesets(owner, slug, lastChangesetNode, DEFAULT_CHANGESETS_LIMIT);
    }
    
    public Iterable<BitbucketChangeset> getChangesets(final String owner, final String slug, final String lastChangesetNode,
            final int changesetsLimit)
    {
        return new Iterable<BitbucketChangeset>() {

            @Override
            public Iterator<BitbucketChangeset> iterator()
            {
                return new BitbucketChangesetIterator(owner, slug, lastChangesetNode, changesetsLimit);
            }
        };
    }
    
    private List<BitbucketChangeset> getChangesetsPrivate(String owner, String slug, String startNode, int limit)
    {
        String getChangesetsWithPageAndLimitUrl = String.format("/repositories/%s/%s/changesets", owner, slug);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("start", startNode);
        parameters.put("limit", "" + limit);
        
        RemoteResponse response = requestor.get(getChangesetsWithPageAndLimitUrl, parameters);
        
        BitbucketChangesetEnvelope bitbucketChangesetEnvelope = ClientUtils.fromJson(response.getResponse(),
                                                                                     BitbucketChangesetEnvelope.class);
        
        return bitbucketChangesetEnvelope.getChangesets();
    }
    
    private final class BitbucketChangesetIterator implements Iterator<BitbucketChangeset> {
        private final String owner;
        private final String slug;
        private final String lastChangesetNode;
        private final int    changesetsLimit;
        
        private Iterator<BitbucketChangeset> changesetsCurrentPage;
        private String nextChangesetNodeToQuery;

        
        private BitbucketChangesetIterator(String owner, String slug, final String lastChangesetNode,
                final int changesetsLimit)
        {
            this.owner = owner;
            this.slug = slug;
            this.lastChangesetNode = lastChangesetNode;
            this.changesetsLimit = changesetsLimit;
            
            List<BitbucketChangeset> changesets = getChangesetsPrivate(owner, slug, "tip", changesetsLimit);
            nextChangesetNodeToQuery = changesets.get(0).getNode();
            
            changesets = reverse(changesets); // because changesets in every page will be returned with lowest date first
            changesetsCurrentPage = filterUntilChangesetNode(changesets).iterator();
        }

        
        @Override
        public boolean hasNext()
        {
            return changesetsCurrentPage.hasNext() || hasMorePages();
        }

        @Override
        public BitbucketChangeset next()
        {
            return changesetsCurrentPage.next();
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
        private boolean hasMorePages()
        {
            List<BitbucketChangeset> changesets = getChangesetsPrivate(owner, slug, nextChangesetNodeToQuery, changesetsLimit);
            nextChangesetNodeToQuery = changesets.get(0).getNode();
            
            changesets.remove(changesets.size() - 1); // because the nextChangesetNodeToQuery is included as last item
            
            changesets = reverse(changesets); // because changesets in every page will be returned with lowest date first
            changesets = filterUntilChangesetNode(changesets);
            changesetsCurrentPage = changesets.iterator();
            
            return !changesets.isEmpty();
        }
        
        private List<BitbucketChangeset> reverse(List<BitbucketChangeset> bitbucketChangesets)
        {
            return Lists.reverse(bitbucketChangesets);
        }
        
        private List<BitbucketChangeset> filterUntilChangesetNode(List<BitbucketChangeset> changesetsToFilter)
        {
            List<BitbucketChangeset> filteredChangesets = new ArrayList<BitbucketChangeset>();
            
            for (BitbucketChangeset bitbucketChangeset : changesetsToFilter)
            {
                if (lastChangesetNode.equals(bitbucketChangeset.getNode()))
                {
                    break;
                }
                else
                {
                    filteredChangesets.add(bitbucketChangeset);
                }
            }
            
            return filteredChangesets;
        }
    }
}

