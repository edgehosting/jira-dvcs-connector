package com.atlassian.jira.plugins.bitbucket.common.bitbucket;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.Authentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.AuthenticationFactory;
import com.atlassian.jira.plugins.bitbucket.bitbucket.Bitbucket;
import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.common.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.common.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.mapper.RepositoryPersister;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class BitbucketRepositoryManager implements RepositoryManager
{
	private final RepositoryPersister repositoryPersister;

	private final Bitbucket bitbucket;
	
	/* Maps ProjectMapping to SourceControlRepository */
	private static final Function<ProjectMapping, SourceControlRepository> TO_SOURCE_CONTROL_REPOSITORY = 
			new Function<ProjectMapping, SourceControlRepository>()
			{
				public SourceControlRepository apply(ProjectMapping pm)
				{
					return new BitbucketRepository(pm.getID(), RepositoryUri.parse(pm.getRepositoryUri()).getRepositoryUrl(),
							pm.getProjectKey(), pm.getUsername(), pm.getPassword());
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
					Authentication auth = authenticationFactory.getAuthentication(repository);
					return bitbucket.getChangeset(uri.getRepositoryUrl(), auth, from.getNode());
				}
			};

	private AuthenticationFactory authenticationFactory;

	public BitbucketRepositoryManager(RepositoryPersister repositoryPersister, Bitbucket bitbucket, AuthenticationFactory authenticationFactory)
	{
		this.repositoryPersister = repositoryPersister;
		this.bitbucket = bitbucket;
		this.authenticationFactory = authenticationFactory;
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

        ProjectMapping pm = repositoryPersister.addRepository(projectKey, RepositoryUri.parse(repositoryUrl), username, password);
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


}
