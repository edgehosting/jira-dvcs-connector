package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;


import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepositoryRemoteRestpoint
{
	private final RemoteRequestor requestor;

	public static class ScmType
	{
		public static final String HG = "hg";
		public static final String GIT = "git";
	}
	
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
                BitbucketRepositoryEnvelope envelope = ClientUtils.fromJson(response.getResponse(),
                        new TypeToken<BitbucketRepositoryEnvelope>(){}.getType());
                return  envelope.getRepositories();
            }
        });
	}
	
	public BitbucketRepository getRepository(String owner, String repositorySlug)
	{
		String getRepositoryUrl = URLPathFormatter.format("/repositories/%s/%s", owner, repositorySlug);
		
		return requestor.get(getRepositoryUrl, null, new ResponseCallback<BitbucketRepository>()
        {
            @Override
            public BitbucketRepository onResponse(RemoteResponse response)
            {
            	BitbucketRepository repository = ClientUtils.fromJson(response.getResponse(),
                        new TypeToken<BitbucketRepository>(){}.getType());
                return repository;
            }
        });
	}
	
	public void removeRepository(String owner, String repositoryName)
    {
        Map<String, String> removeRepoPostData = new HashMap<String, String>();
        removeRepoPostData.put("repo_slug",   repositoryName);
        removeRepoPostData.put("accountname", owner);

        String removeRepositoryUrl = String.format("/repositories/%s/%s", owner, repositoryName);

        requestor.delete(removeRepositoryUrl, removeRepoPostData, ResponseCallback.EMPTY);
    }
	
    public BitbucketRepository forkRepository(String owner, String repositoryName, String newRepositoryName, boolean isPrivate)
    {
    	Map<String, String> createRepoPostData = new HashMap<String, String>();
        createRepoPostData.put("name", newRepositoryName);
        createRepoPostData.put("is_private", Boolean.toString(isPrivate));

        String forkRepositoryUrl = String.format("/repositories/%s/%s/fork", owner, repositoryName);
                
        return requestor.post(forkRepositoryUrl, createRepoPostData, new ResponseCallback<BitbucketRepository>()
        {
            @Override
            public BitbucketRepository onResponse(RemoteResponse response)
            {
            	BitbucketRepository repository = ClientUtils.fromJson(response.getResponse(),
                        new TypeToken<BitbucketRepository>(){}.getType());
                return repository;
            }
        });
    }
    
    public BitbucketRepository createRepository(String repositoryName, String scm, boolean isPrivate)
    {
        Map<String, String> createRepoPostData = new HashMap<String, String>();
        createRepoPostData.put("name", repositoryName);
        createRepoPostData.put("scm",  scm);
        createRepoPostData.put("is_private", Boolean.toString(isPrivate));

        return requestor.post("/repositories", createRepoPostData, new ResponseCallback<BitbucketRepository>()
        {
            @Override
            public BitbucketRepository onResponse(RemoteResponse response)
            {
            	BitbucketRepository repository = ClientUtils.fromJson(response.getResponse(),
                        new TypeToken<BitbucketRepository>(){}.getType());
                return repository;
            }
        });
    }
    
    public BitbucketRepository createRepository(String owner, String repositoryName, String scm, boolean isPrivate)
    {
        Map<String, String> createRepoPostData = new HashMap<String, String>();
        createRepoPostData.put("name", repositoryName);
        createRepoPostData.put("scm",  scm);
        createRepoPostData.put("is_private", Boolean.toString(isPrivate));
        
        String createRepositoryUrl = String.format("/repositories/%s/%s", owner, repositoryName);
        
        return requestor.put(createRepositoryUrl, createRepoPostData, new ResponseCallback<BitbucketRepository>()
        {
            @Override
            public BitbucketRepository onResponse(RemoteResponse response)
            {
            	BitbucketRepository repository = ClientUtils.fromJson(response.getResponse(),
                        new TypeToken<BitbucketRepository>(){}.getType());
                return repository;
            }
        });
    }
}

