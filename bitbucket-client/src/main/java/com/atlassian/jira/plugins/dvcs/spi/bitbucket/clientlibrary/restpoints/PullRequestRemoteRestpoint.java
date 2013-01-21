package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.Date;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityIterator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

/**
 * 
 * PullRequestRemoteRestpoint
 *
 * 
 * <br /><br />
 * Created on 11.12.2012, 13:14:31
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class PullRequestRemoteRestpoint
{

    private final RemoteRequestor requestor;

    public PullRequestRemoteRestpoint(RemoteRequestor requestor)
    {
        this.requestor = requestor;
    }
    
    public Iterable<BitbucketPullRequestActivityInfo> getRepositoryActivity(String owner, String repoSlug, Date upToDate) {

        return new BitbucketPullRequestActivityIterator(requestor, upToDate, repoSlug, owner);

    }
    
    public BitbucketPullRequest getPullRequestDetail(String owner, String repoSlug, String localId) {

        String url = String.format("/repositories/%s/%s/pullrequests/%s", owner, repoSlug, localId);

        return requestor.get(url, null, new ResponseCallback<BitbucketPullRequest>()
        {

            @Override
            public BitbucketPullRequest onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<Set<BitbucketPullRequest>>()
                {
                }.getType());
            }

        });
        
    }

}

