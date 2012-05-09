package com.atlassian.jira.plugins.dvcs.spi.github;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;

public class GithubCommunicator implements DvcsCommunicator
{
    private static final Logger log = LoggerFactory.getLogger(GithubCommunicator.class);

    public static final String GITHUB = "github";

    private final GithubOAuth githubOAuth;

    public GithubCommunicator(GithubOAuth githubOAuth)
    {
        this.githubOAuth = githubOAuth;
    }

    @Override
    public String getDvcsType()
    {
        return GITHUB;
    }

    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        UserService userService = new UserService(GitHubClient.createClient(hostUrl));
        try
        {
            User user = userService.getUser(accountName);
            boolean requiresOauth = StringUtils.isBlank(githubOAuth.getClientId()) || StringUtils.isBlank(githubOAuth.getClientSecret());

            return new AccountInfo(GithubCommunicator.GITHUB, requiresOauth);
        } catch (IOException e)
        {
            log.debug("Unable to retrieve account information ", e);
        }
        return null;

    }

    @Override
    public List<Repository> getRepositories(Organization organization)
    {
        RepositoryService repositoryService = new RepositoryService(GitHubClient.createClient(organization.getHostUrl()));
        repositoryService.getClient().setOAuth2Token(organization.getCredential().getAccessToken());
        try
        {
            List<org.eclipse.egit.github.core.Repository> ghRepositories = repositoryService.getRepositories(organization.getName());
            List<Repository> repositories = new ArrayList<Repository>();
            for (org.eclipse.egit.github.core.Repository ghRepository : ghRepositories)
            {
                Repository repository = new Repository();
                repository.setSlug(ghRepository.getName());
                repository.setName(ghRepository.getName());
                repositories.add(repository);
            }
            log.debug("Found repositories: " + repositories.size());
            return repositories;
        } catch (IOException e)
        {
            throw new SourceControlException("Error retrieving list of repositories", e);
        }

    }

	@Override
	public Changeset getDetailChangeset(Organization organization, Repository repository, Changeset changeset)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<Changeset> getChangesets(Organization organization, Repository repository, Date lastCommitDate)
	{
		// TODO Auto-generated method stub
		return null;
	}
    
    
}
