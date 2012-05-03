package com.atlassian.jira.plugins.dvcs.spi.github;

import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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
}
