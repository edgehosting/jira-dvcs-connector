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

    BitbucketClientBuilder apiVersion(int apiVersion);

    BitbucketClientBuilder timeout(int timeout);

    BitbucketRemoteClient build();
}
