package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.JsonParsingException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

/**
 * RepositoryRemoteRestpoint
 * 
 * 
 * <br />
 * <br />
 * Created on 13.7.2012, 17:28:55 <br />
 * <br />
 * 
 * @author jhocman@atlassian.com
 * 
 */
public class RepositoryRemoteRestpoint
{
    private final RemoteRequestor requestor;

    public RepositoryRemoteRestpoint(RemoteRequestor requestor)
    {
        super();
        this.requestor = requestor;
    }

    public List<BitbucketRepository> getAllRepositories(final String owner)
    {
        String getAllRepositoriesUrl = URLPathFormatter.format("/users/%s", owner);

        return requestor.get(getAllRepositoriesUrl, null, new ResponseCallback<List<BitbucketRepository>>()
        {
            @Override
            public List<BitbucketRepository> onResponse(RemoteResponse response)
            {
                try
                {
                    BitbucketRepositoryEnvelope envelope = ClientUtils.fromJson(response.getResponse(),
                            new TypeToken<BitbucketRepositoryEnvelope>()
                            {
                            }.getType());
                    return envelope.getRepositories();

                } catch (JsonParsingException e)
                {
                    throw new RuntimeException(
                            "Unexpected response was returned back from server side. Check that all provided information of account '"
                                    + owner + "' is valid. Basically it means a mistake in provided account name.", e);
                }
            }
        });
    }
}
