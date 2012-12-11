package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.ArrayList;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketComment;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;

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
    
    public Iterable<BitbucketPullRequest> getAllPullRequestsForRepository(String owner, String repoSlug) {
        // TODO
        return new ArrayList<BitbucketPullRequest>();
    }
    
    public Iterable<BitbucketComment> getAllCommentsForPullRequest(Integer pullRequestId) {
        // TODO
        return new ArrayList<BitbucketComment>();
    }

    
    public Iterable<BitbucketComment> getAllCommentsForChangeset(String owner, String repoSlug, String rawNode) {
        // TODO
        return new ArrayList<BitbucketComment>();
    }
    

}

