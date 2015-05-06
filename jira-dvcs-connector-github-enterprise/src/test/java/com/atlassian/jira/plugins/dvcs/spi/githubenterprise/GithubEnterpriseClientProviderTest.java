package com.atlassian.jira.plugins.dvcs.spi.githubenterprise;

import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientWithTimeout;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseClientProvider.createClientForGithubEnteprise;
import static org.testng.Assert.assertNull;

public class GithubEnterpriseClientProviderTest
{
    @Test
    public void getRateLimitShouldReturnNullForGithubEnterprise() throws Exception
    {
        GithubClientWithTimeout githubClient = createClientForGithubEnteprise("http://github.prod.inf.atlassian.com", "agent");
        assertNull(githubClient.getRateLimit());
    }
}