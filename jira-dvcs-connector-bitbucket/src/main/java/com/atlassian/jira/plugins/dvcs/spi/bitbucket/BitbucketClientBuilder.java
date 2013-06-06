package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;

/**
 * BitbucketClientBuilder interface
 * 
 * @author Miroslav Stencel <mstencel@atlassian.com>
 */
public interface BitbucketClientBuilder
{
    BitbucketClientBuilder forOrganization(Organization organization);

    BitbucketClientBuilder forRepository(Repository repository);

    BitbucketClientBuilder noAuthClient(String hostUrl);
    
    BitbucketClientBuilder authClient(String hostUrl, String name, Credential credential);
    
    BitbucketClientBuilder cached();
    
    BitbucketClientBuilder apiVersion(int apiVersion);
    
    BitbucketRemoteClient build();
}
