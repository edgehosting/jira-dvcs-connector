package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.google.gson.reflect.TypeToken;

public class RepositoryRemoteRestpoint
{

	private final RemoteRequestor requestor;

	public RepositoryRemoteRestpoint(RemoteRequestor requestor)
	{
		super();
		this.requestor = requestor;
	}
	
	public List<BitbucketRepository> getAllRepositories(String forAccount) {
		
		RemoteResponse response = requestor.get("/users/" + forAccount, null);
		
		BitbucketRepositoryEnvelope envelope = ClientUtils.fromJson(response.getResponse() , new TypeToken<BitbucketRepositoryEnvelope>(){}.getType());
		
		return envelope.getRepositories();
	}
	
}

