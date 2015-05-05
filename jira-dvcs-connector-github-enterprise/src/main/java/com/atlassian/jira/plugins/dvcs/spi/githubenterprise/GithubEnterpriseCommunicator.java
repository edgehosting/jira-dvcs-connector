package com.atlassian.jira.plugins.dvcs.spi.githubenterprise;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.EnumSet;

@Component("githubEnterpriseCommunicator")
public class GithubEnterpriseCommunicator extends GithubCommunicator
{
    private static final Logger log = LoggerFactory.getLogger(GithubEnterpriseCommunicator.class);
    public static final String GITHUB_ENTERPRISE = "githube";

    @Autowired
    private GithubEnterpriseCommunicator(OAuthStore oAuthStore,
            @Qualifier ("githubEnterpriseClientProvider") GithubEnterpriseClientProvider githubClientProvider)
    {
        super(oAuthStore, githubClientProvider);
    }

    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        UserService userService = new UserService(githubClientProvider.createClient(hostUrl));
        try
        {
            userService.getUser(accountName);
            return new AccountInfo(GithubCommunicator.GITHUB);
        }
        catch (RequestException e)
        {
            log.debug("Unable to retrieve account information. hostUrl: {}, account: {} " + e.getMessage(),
                    hostUrl, accountName);

            // GitHub Enterprise returns a 403 status for unauthorized requests.
            if (e.getStatus() == 403)
            {
                return new AccountInfo(GithubCommunicator.GITHUB);
            }

        }
        catch (IOException e)
        {
            log.debug("Unable to retrieve account information. hostUrl: {}, account: {} " + e.getMessage(),
                    hostUrl, accountName);
        }
        return null;
    }

    @Override
    public String getDvcsType()
    {
        return GITHUB_ENTERPRISE;
    }

    public boolean isSyncDisabled(final Repository repo, final EnumSet<SynchronizationFlag> flags)
    {
        return syncDisabledHelper.isGithubEnterpriseSyncDisabled();
    }
}

