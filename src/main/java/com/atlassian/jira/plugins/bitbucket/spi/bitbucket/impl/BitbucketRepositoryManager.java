package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.api.*;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.ApplicationProperties;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
        if (!hasValidFormat(url)) return false;
        RepositoryUri repositoryUri = getRepositoryUri(url);

        return getCommunicator().isRepositoryValid(repositoryUri);
	}

    public RepositoryUri getRepositoryUri(String urlString)
    {
        try
        {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            String hostname = url.getHost();
            String path = url.getPath();
            String[] split = StringUtils.split(path, "/");
            if (split.length<2)
            {
                throw new SourceControlException("Expected url is https://domainname.com/username/repository");
            }
            String owner = split[0];
            String slug = split[1];
            return new BitbucketRepositoryUri(protocol, hostname, owner, slug);
        }
        catch (MalformedURLException e)
        {
            throw new SourceControlException("Invalid url ["+urlString+"]");
        }

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
