package com.atlassian.jira.plugins.dvcs.smartcommits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

public class DefaultSmartcommitsPayloadParser implements SmartcommitsPayloadParser
{

	private static Map<String, SmartcommitsPayloadParser> dvcsTypeToParsers = new HashMap<String, SmartcommitsPayloadParser>();
	static {
		dvcsTypeToParsers.put(BitbucketCommunicator.BITBUCKET, new BitbucketSmartcommitsPayloadParser());
		dvcsTypeToParsers.put(GithubCommunicator.GITHUB, new GithubSmartcommitsPayloadParser());
	}
	
	private final RepositoryService repositoryService;
	
	public DefaultSmartcommitsPayloadParser(RepositoryService repositoryService)
	{
		super();
		this.repositoryService = repositoryService;
	}

	@Override
	public List<PayloadChangeset> parse(String payload, int repositoryId)
	{
		Repository repository = repositoryService.get(repositoryId);
		
		if (repository != null && dvcsTypeToParsers.get(repository.getDvcsType()) != null) {
		
			return dvcsTypeToParsers.get(repository.getDvcsType()).parse(payload, repositoryId);

		} else {
			
			return new ArrayList<PayloadChangeset>();
		}
		
	}

	
	static class BitbucketSmartcommitsPayloadParser implements SmartcommitsPayloadParser {

		@Override
		public List<PayloadChangeset> parse(String payload, int repositoryId)
		{
			List<PayloadChangeset> changesets = new ArrayList<PayloadChangeset>();
	        
			try
	        {
	            JSONObject jsonPayload = new JSONObject(payload);
	            JSONArray commits = jsonPayload.getJSONArray("commits");

	            for (int i = 0; i < commits.length(); ++i)
	            {
	                JSONObject commitJson = commits.getJSONObject(i);
	                String commitMessage = commitJson.getString("message");
	                String commitAuthor = commitJson.getString("author");
	                
	                PayloadChangeset changeset = new PayloadChangeset();
	                changeset.setAuthor(commitAuthor);
	                changeset.setCommitMessage(commitMessage);
	                
					changesets.add(changeset);
	            }
	        
	        } catch (JSONException e)
	        {
	            throw new SourceControlException("Error parsing payload: " + payload, e);
	        
	        }
	        return changesets;
		}
		
	}

	static class GithubSmartcommitsPayloadParser implements SmartcommitsPayloadParser {
		
		@Override
		public List<PayloadChangeset> parse(String payload, int repositoryId)
		{
			
			List<PayloadChangeset> changesets = new ArrayList<PayloadChangeset>();
	        
			try
	        {
	            JSONObject jsonPayload = new JSONObject(payload);
	            JSONArray commits = jsonPayload.getJSONArray("commits");

	            for (int i = 0; i < commits.length(); ++i)
	            {
	                JSONObject commitJson = commits.getJSONObject(i);
	                String commitMessage = commitJson.getString("message");
	                String commitAuthor = commitJson.getJSONObject("author").getString("name");
	                
	                PayloadChangeset changeset = new PayloadChangeset();
	                changeset.setAuthor(commitAuthor);
	                changeset.setCommitMessage(commitMessage);
	                
					changesets.add(changeset);
	            }
	        
	        } catch (JSONException e)
	        {
	            throw new SourceControlException("Error parsing payload: " + payload, e);
	        
	        }
	        return changesets;
		}
		
	}
}

