package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;

/**
 * BitbucketClientBuilder interface
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
public interface BitbucketClientBuilder
{
    BitbucketClientBuilder cached();

    BitbucketClientBuilder closeIdleConnections();

    BitbucketClientBuilder apiVersion(int apiVersion);

    BitbucketRemoteClient build();
}
