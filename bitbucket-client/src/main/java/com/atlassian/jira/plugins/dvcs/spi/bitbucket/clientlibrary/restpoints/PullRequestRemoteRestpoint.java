package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.Date;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityIterator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivity;
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
    
    public Iterable<BitbucketPullRequestBaseActivity> getRepositoryActivity(String owner, String repoSlug, Date upToDate) {

        return new BitbucketPullRequestActivityIterator(requestor, upToDate, repoSlug, owner);

    }
    

}

