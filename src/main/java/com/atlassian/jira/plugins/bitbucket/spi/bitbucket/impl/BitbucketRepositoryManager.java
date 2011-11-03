package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.api.*;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.ApplicationProperties;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BitbucketRepositoryManager extends DvcsRepositoryManager
{

	public BitbucketRepositoryManager(RepositoryPersister repositoryPersister, @Qualifier("bitbucketCommunicator") Communicator communicator, Encryptor encryptor, ApplicationProperties applicationProperties)
	{
        super(communicator, repositoryPersister, encryptor, applicationProperties);
	}

    public String getRepositoryType()
    {
        return "bitbucket";
    }

	public boolean canHandleUrl(String url)
	{
        // Valid URL 
        Pattern p = Pattern.compile("^(https|http)://[a-zA-Z0-9][-a-zA-Z0-9]*.[a-zA-Z0-9]+/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        Matcher m = p.matcher(url);
        return m.matches();
	}


	public List<Changeset> parsePayload(SourceControlRepository repository, String payload)
	{
        List<Changeset> changesets = new ArrayList<Changeset>();
        try
		{
			JSONObject jsonPayload = new JSONObject(payload);
			JSONArray commits = jsonPayload.getJSONArray("commits");

			for (int i = 0; i < commits.length(); ++i)
			{
				changesets.add(BitbucketChangesetFactory.parse(repository.getId(), commits.getJSONObject(i)));
			}
		} catch (JSONException e)
		{
			throw new SourceControlException(e);
		}
        return changesets;

	}

	public void setupPostcommitHook(SourceControlRepository repo)
	{
		getCommunicator().setupPostcommitHook(repo, getPostCommitUrl(repo));
	}

	private String getPostCommitUrl(SourceControlRepository repo)
	{
		return getApplicationProperties().getBaseUrl() + "/rest/bitbucket/1.0/repository/"+repo.getId()+"/sync";
	}

	public void removePostcommitHook(SourceControlRepository repo)
	{
		getCommunicator().removePostcommitHook(repo, getPostCommitUrl(repo));
	}

}
