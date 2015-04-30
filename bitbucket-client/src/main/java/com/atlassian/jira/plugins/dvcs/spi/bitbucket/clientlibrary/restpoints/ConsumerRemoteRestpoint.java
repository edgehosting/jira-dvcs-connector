package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketConsumer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;

import java.util.List;

import javax.annotation.Nonnull;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * REST service to retrieve information about the OAUTH consumers in Bitbucket.
 */
public class ConsumerRemoteRestpoint
{
    private final RemoteRequestor requestor;

    public ConsumerRemoteRestpoint(@Nonnull final RemoteRequestor remoteRequestor)
    {
        this.requestor = Preconditions.checkNotNull(remoteRequestor);
    }

    public List<BitbucketConsumer> getConsumers(@Nonnull final String owner)
    {
        Preconditions.checkState(isNotBlank(owner), "Mandatory owner parameter is undefined!");

        final String uri = URLPathFormatter.format("/users/%s/consumers", owner);

        return requestor.get(uri, null, new ResponseCallback<List<BitbucketConsumer>>()
        {

            @Override
            public List<BitbucketConsumer> onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<List<BitbucketConsumer>>()
                {
                }.getType());
            }
        });
    }
}
