package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.google.gson.reflect.TypeToken;

/**
 * ChangesetRemoteRestpoint
 * 
 * @author Martin Skurla mskurla@atlassian.com
 */
public class ChangesetRemoteRestpoint
{
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
        if (limit != DIFFSTAT_NO_LIMIT)
        {
            getChangesetDiffStatUrl += ("?limit=" + limit);
        }
        
        RemoteResponse response = requestor.get(getChangesetDiffStatUrl, null);
        
        return ClientUtils.fromJson(response.getResponse(),
                                    new TypeToken<List<BitbucketChangesetWithDiffstat>>(){}.getType());
    }
}

