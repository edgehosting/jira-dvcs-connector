package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.jira.plugins.bitbucket.Progress;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.SynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class BitbucketRepositoryManager implements RepositoryManager
{
	private final RepositoryPersister repositoryPersister;
	private final BitbucketCommunicator bitbucket;
	private final Encryptor encryptor;
	
	
	/* Maps ProjectMapping to SourceControlRepository */
	public final Function<ProjectMapping, SourceControlRepository> TO_SOURCE_CONTROL_REPOSITORY = 
			new Function<ProjectMapping, SourceControlRepository>()
			{
				public SourceControlRepository apply(ProjectMapping pm)
				{
					String decryptedPassword = encryptor.decrypt(pm.getPassword(), pm.getProjectKey(), pm.getRepositoryUri());
					return new DefaultSourceControlRepository(pm.getID(), RepositoryUri.parse(pm.getRepositoryUri()).getRepositoryUrl(),
							pm.getProjectKey(), pm.getUsername(), decryptedPassword);
				}
			};

	private final Function<IssueMapping, Changeset> TO_CHANGESET = 
			new Function<IssueMapping, Changeset>()
			{
				public Changeset apply(IssueMapping from)
				{
					RepositoryUri uri = RepositoryUri.parse(from.getRepositoryUri());
					ProjectMapping pm = repositoryPersister.getRepository(from.getProjectKey(), uri);
                    SourceControlRepository repository = TO_SOURCE_CONTROL_REPOSITORY.apply(pm);
					return bitbucket.getChangeset(repository, from.getNode());
				}
			};


	public BitbucketRepositoryManager(RepositoryPersister repositoryPersister, BitbucketCommunicator bitbucket, Encryptor encryptor)
	{
		this.repositoryPersister = repositoryPersister;
		this.bitbucket = bitbucket;
		this.encryptor = encryptor;
	}

	public boolean canHandleUrl(String url)
	{
        // Valid URL and URL starts with bitbucket.org domain
        Pattern p = Pattern.compile("^(https|http)://bitbucket.org/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        Matcher m = p.matcher(url);
        return m.matches();
	}

	public SourceControlRepository addRepository(String projectKey, String repositoryUrl, String username, String password)
	{
        // Remove trailing slashes from URL
        if (repositoryUrl.endsWith("/"))
        {
            repositoryUrl = repositoryUrl.substring(0, repositoryUrl.length() - 1);
        }

        // Set all URLs to HTTPS
        if (repositoryUrl.startsWith("http:"))
        {
            repositoryUrl = repositoryUrl.replaceFirst("http:", "https:");
        }

        String encryptedPassword = encryptor.encrypt(password, projectKey, repositoryUrl);
        ProjectMapping pm = repositoryPersister.addRepository(projectKey, RepositoryUri.parse(repositoryUrl), username, encryptedPassword);
        return TO_SOURCE_CONTROL_REPOSITORY.apply(pm);
	}


	public List<SourceControlRepository> getRepositories(String projectKey)
	{
		 List<ProjectMapping> repositories = repositoryPersister.getRepositories(projectKey);
		 return Lists.transform(repositories, TO_SOURCE_CONTROL_REPOSITORY);
	}

	public SourceControlRepository getRepository(String projectKey, String repositoryUrl)
	{
		ProjectMapping repository = repositoryPersister.getRepository(projectKey, RepositoryUri.parse(repositoryUrl));
		return TO_SOURCE_CONTROL_REPOSITORY.apply(repository);
	}

	public List<Changeset> getChangesets(String issueKey)
	{
		List<IssueMapping> issueMappings = repositoryPersister.getIssueMappings(issueKey);
		return Lists.transform(issueMappings, TO_CHANGESET);
	}

	public void removeRepository(String projectKey, String url)
	{
		repositoryPersister.removeRepository(projectKey, RepositoryUri.parse(url));
        // Should we also delete IssueMappings?
	}

	public void addChangeset(String issueId, Changeset changeset)
	{
		repositoryPersister.addChangeset(issueId, changeset);
	}

	public SourceControlUser getUser(String repositoryUrl, String username)
	{
		return bitbucket.getUser(username);
	}

	public SynchronisationOperation getSynchronisationOperation(SynchronizationKey key, Function<SynchronizationKey, Progress> progressProvider)
	{
		return new BitbucketSynchronisation(key, this, bitbucket, progressProvider);
	}
}
