package com.atlassian.jira.plugins.dvcs.remoterestpoint;

import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

/**
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
public class PullRequestLocalRestpoint
{
    private final RemoteRequestor requestor;

    public PullRequestLocalRestpoint(RemoteRequestor requestor)
    {
        this.requestor = requestor;
    }

    public RestDevResponse<RestPrRepository> getPullRequest(String baseUrl, String issueKey)
    {
        return requestor.get(baseUrl + "/rest/bitbucket/1.0/jira-dev/pr-detail?issue="+issueKey, null, new ResponseCallback<RestDevResponse<RestPrRepository>>()
        {
            @Override
            public RestDevResponse<RestPrRepository> onResponse(final RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<RestDevResponse<RestPrRepository>>()
                {
                }.getType());
            }
        });
    }
}
