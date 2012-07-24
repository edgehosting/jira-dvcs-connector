package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;


import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;


/**
 * RepositoryLinkRemoteRestpoint
 * 
 * @author Martin Skurla mskurla@atlassian.com
 */
public class RepositoryLinkRemoteRestpoint
{
    private final RemoteRequestor requestor;
    

	public RepositoryLinkRemoteRestpoint(RemoteRequestor remoteRequestor)
	{
		this.requestor = remoteRequestor;
	}
    
    
    public List<BitbucketRepositoryLink> getRepositoryLinks(String owner, String slug)
    {
        String getRepositoryLinksUrl = String.format("/repositories/%s/%s/links", owner, slug);
        
        RemoteResponse response = requestor.get(getRepositoryLinksUrl, null);

        return ClientUtils.fromJson(response.getResponse(),
                                    new TypeToken<List<BitbucketRepositoryLink>>(){}.getType());
    }
    
    public void removeRepositoryLink(String owner, String slug, int id)
    {
        String removeRepositoryLinkUrl = String.format("/repositories/%s/%s/links/%d", owner, slug, id);

        requestor.delete(removeRepositoryLinkUrl);
    }
    
    public BitbucketRepositoryLink addRepositoryLink(String owner, String slug, String handler, String linkUrl, String linkKey)
    {
        String addRepositoryUrl = String.format("/repositories/%s/%s/links", owner, slug);

        Map<String, String> params = Maps.newHashMap();
        params.put("handler",  handler);
        params.put("link_url", linkUrl);
        params.put("link_key", linkKey);
        
        RemoteResponse response = requestor.post(addRepositoryUrl, params);

        return ClientUtils.fromJson(response.getResponse(),
                                    new TypeToken<BitbucketRepositoryLink>(){}.getType());
    }
    
//    public BitbucketRepositoryLink addCustomRepositoryLink(String owner, String slug, String handler, String linkUrl, String linkKey)
//    {
//        String addRepositoryUrl = String.format("/repositories/%s/%s/links", owner, slug);
//
//        Map<String, String> params = Maps.newHashMap();
//        params.put("handler",  "custom");
//        params.put("link_url", replacementUrl);//linkurl+/browse/\1
//        params.put("link_key", regexp);//regex natvrdo
//        
//        RemoteResponse response = requestor.post(addRepositoryUrl, params);
//
//        return ClientUtils.fromJson(response.getResponse(),
//                                    new TypeToken<BitbucketRepositoryLink>(){}.getType());
//    }
    
    BitbucketRepositoryLink getRepositoryLink(String owner, String slug, int id)
    {       
        String getRepositoryLinkUrl = String.format("/repositories/%s/%s/links/%d", owner, slug, id);
        
        RemoteResponse response = requestor.get(getRepositoryLinkUrl, null);
        
        return ClientUtils.fromJson(response.getResponse(),
                                    new TypeToken<BitbucketRepositoryLink>(){}.getType());
    }
}

