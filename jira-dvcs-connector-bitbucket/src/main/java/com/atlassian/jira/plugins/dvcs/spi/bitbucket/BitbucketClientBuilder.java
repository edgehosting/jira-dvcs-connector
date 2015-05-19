package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;

public interface BitbucketClientBuilder
{
    BitbucketClientBuilder cached();

    BitbucketClientBuilder closeIdleConnections();

    BitbucketClientBuilder apiVersion(int apiVersion);

    BitbucketRemoteClient build();
}
