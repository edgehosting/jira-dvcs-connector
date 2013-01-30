package com.atlassian.jira.plugins.dvcs.spi.github;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubRepositoryService;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.GithubOAuthUtils;

public class GithubEnterpriseCommunicator extends GithubCommunicator
{
    private static final Logger log = LoggerFactory.getLogger(GithubEnterpriseCommunicator.class);
    
    public static final String GITHUB_ENTERPRISE = "githube";

    private GithubEnterpriseCommunicator(ChangesetCache changesetCache, GithubOAuth githubOAuth,
            GithubClientProvider githubClientProvider, GitHubRepositoryService gitHubRepositoryService)
    {
        super(changesetCache, githubOAuth, githubClientProvider, gitHubRepositoryService);
    }
        
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        UserService userService = new UserService(GithubOAuthUtils.createClient(hostUrl));
        boolean requiresOauth = StringUtils.isBlank(githubOAuth.getEnterpriseClientId()) || StringUtils.isBlank(githubOAuth.getEnterpriseClientSecret());

        try
        {
            userService.getUser(accountName);

            return new AccountInfo(GithubCommunicator.GITHUB, requiresOauth);

        } catch (RequestException e)
        {
            log.debug("Unable to retrieve account information. hostUrl: {}, account: {} " + e.getMessage(),
                    hostUrl, accountName);

            // GitHub Enterprise returns a 403 status for unauthorized requests.
            if (e.getStatus() == 403)
            {
                return new AccountInfo(GithubCommunicator.GITHUB, requiresOauth);
            }

        } catch (IOException e)
        {
            log.debug("Unable to retrieve account information. hostUrl: {}, account: {} " + e.getMessage(),
                    hostUrl, accountName);
        }
        return null;

    }

    @Override
    public boolean isOauthConfigured()
    {
        return StringUtils.isNotBlank(githubOAuth.getEnterpriseHostUrl())
                && StringUtils.isNotBlank(githubOAuth.getEnterpriseClientId())
                && StringUtils.isNotBlank(githubOAuth.getEnterpriseClientSecret());
    }
    
    @Override
    public String getDvcsType()
    {
        return GITHUB_ENTERPRISE;
    }
}

