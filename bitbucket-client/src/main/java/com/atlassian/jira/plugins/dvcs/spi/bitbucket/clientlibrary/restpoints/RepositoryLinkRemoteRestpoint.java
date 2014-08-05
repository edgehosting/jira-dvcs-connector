package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        String getRepositoryLinksUrl = URLPathFormatter.format("/repositories/%s/%s/links", owner, slug);

        return requestor.get(getRepositoryLinksUrl, null, new ResponseCallback<List<BitbucketRepositoryLink>>()
        {

            @Override
            public List<BitbucketRepositoryLink> onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<List<BitbucketRepositoryLink>>()
                {
                }.getType());
            }

        });

    }

    public void removeRepositoryLink(String owner, String slug, int id)
    {
        String removeRepositoryLinkUrl = URLPathFormatter.format("/repositories/%s/%s/links/%s", owner, slug, "" + id);

        requestor.delete(removeRepositoryLinkUrl, Collections.<String, String>emptyMap(), ResponseCallback.EMPTY);
    }

    public BitbucketRepositoryLink addRepositoryLink(String owner, String slug, String handler, String linkUrl,
            String linkKey)
    {
        String addRepositoryUrl = URLPathFormatter.format("/repositories/%s/%s/links", owner, slug);

        Map<String, String> params = Maps.newHashMap();
        params.put("handler", handler);
        params.put("link_url", linkUrl);
        params.put("link_key", linkKey);

        return requestor.post(addRepositoryUrl, params, new ResponseCallback<BitbucketRepositoryLink>()
        {

            @Override
            public BitbucketRepositoryLink onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketRepositoryLink>()
                {
                }.getType());
            }

        });

    }

    public BitbucketRepositoryLink addCustomRepositoryLink(String owner, String slug, String replacementUrl, String rex)
    {
        String addRepositoryUrl = URLPathFormatter.format("/repositories/%s/%s/links", owner, slug);

        Map<String, String> params = Maps.newHashMap();
        params.put("handler", "custom");
        params.put("link_url", replacementUrl); // linkurl+/browse/\1
        params.put("link_key", rex);

        return requestor.post(addRepositoryUrl, params, new ResponseCallback<BitbucketRepositoryLink>()
        {
            @Override
            public BitbucketRepositoryLink onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketRepositoryLink>()
                {
                }.getType());
            }
        });
    }

    public BitbucketRepositoryLink getRepositoryLink(String owner, String slug, int id)
    {
        String getRepositoryLinkUrl = URLPathFormatter.format("/repositories/%s/%s/links/%s", owner, slug, "" + id);

        return requestor.get(getRepositoryLinkUrl, null, new ResponseCallback<BitbucketRepositoryLink>()
        {
            @Override
            public BitbucketRepositoryLink onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketRepositoryLink>()
                {
                }.getType());
            }
        });
    }
}
