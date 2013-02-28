package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;

public interface BitbucketClientRemoteFactory
{
    BitbucketRemoteClient getForOrganization(Organization organization);
    BitbucketRemoteClient getForRepository(Repository repository);
    BitbucketRemoteClient getForRepository(Repository repository, int apiVersion);
    BitbucketRemoteClient getNoAuthClient(String hostUrl);
}
