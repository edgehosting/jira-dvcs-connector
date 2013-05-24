package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;

public interface BitbuckeAuthProviderFactory
{
    AuthProvider getForOrganization(Organization organization);

    AuthProvider getForRepository(Repository repository);

    AuthProvider getForRepository(Repository repository, int apiVersion);

    AuthProvider getNoAuthClient(String hostUrl);

    AuthProvider createProvider(String hostUrl, String name, Credential credential);
}
