package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityIterator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivityEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommit;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommitIterator;
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

    public static final int REPO_ACTIVITY_PAGESIZE = 30;

    private final RemoteRequestor requestor;

    public PullRequestRemoteRestpoint(RemoteRequestor requestor)
    {
        this.requestor = requestor;
    }

    public Iterable<BitbucketPullRequestActivityInfo> getRepositoryActivity(String owner, String repoSlug, Date upToDate) {

        return new BitbucketPullRequestActivityIterator(requestor, upToDate, repoSlug, owner);

    }

    public BitbucketPullRequestBaseActivityEnvelope getRepositoryActivityPage(int page, String owner, String repoSlug, final Date upToDate) {

        String activityUrl = String.format("/repositories/%s/%s/pullrequests/activity?pagelen=%s&page=%s", owner, repoSlug, REPO_ACTIVITY_PAGESIZE, page);
        ResponseCallback<BitbucketPullRequestBaseActivityEnvelope> callback = new ResponseCallback<BitbucketPullRequestBaseActivityEnvelope>()
        {
            @Override
            public BitbucketPullRequestBaseActivityEnvelope onResponse(RemoteResponse response)
            {
                BitbucketPullRequestBaseActivityEnvelope remote =
                        ClientUtils.fromJson(response.getResponse(),new TypeToken<BitbucketPullRequestBaseActivityEnvelope>(){}.getType() );

                if (remote != null && remote.getValues() != null && !remote.getValues().isEmpty())
                {
                    // filter by date here
                    remote.setValues(filterByDate(upToDate, remote.getValues()));
                }

                return remote;
            }

            private List<BitbucketPullRequestActivityInfo> filterByDate(Date upToDate, List<BitbucketPullRequestActivityInfo> values)
            {
                List<BitbucketPullRequestActivityInfo> fine = new ArrayList<BitbucketPullRequestActivityInfo>();
                for (BitbucketPullRequestActivityInfo info : values)
                {
                    Date activityDate = ClientUtils.extractActivityDate(info.getActivity());
                    if (upToDate == null || activityDate != null && activityDate.after(upToDate))
                    {
                        fine.add(info);
                    }
                }
                return fine;
            }
        };
        return requestor.get(activityUrl, null, callback);

    }



    public Iterable<BitbucketPullRequestCommit> getPullRequestCommits(String urlIncludingApi) {

        return new BitbucketPullRequestCommitIterator(requestor, urlIncludingApi);
    }


    public BitbucketPullRequest getPullRequestDetail(String owner, String repoSlug, String localId) {

        String url = String.format("/repositories/%s/%s/pullrequests/%s", owner, repoSlug, localId);

        return requestor.get(url, null, new ResponseCallback<BitbucketPullRequest>()
        {

            @Override
            public BitbucketPullRequest onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketPullRequest>()
                {
                }.getType());
            }

        });

    }

}

