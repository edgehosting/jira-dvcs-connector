package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.NoAuthAuthProvider;

/**
 * BitbucketRemoteClientFactory
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public final class BitbucketRemoteClientFactory
{
    private BitbucketRemoteClientFactory() {}
    
    
    public static BitbucketRemoteClient fromOrganization(Organization organization)
    {
        AuthProvider authProvider = new NoAuthAuthProvider(organization.getHostUrl());
        
        return new BitbucketRemoteClient(authProvider);
    }
    
    public static BitbucketRemoteClient fromRepository(Repository repository)
    {
        AuthProvider authProvider = new BasicAuthAuthProvider(repository.getOrgHostUrl(),
                                                              repository.getCredential().getAdminUsername(),
                                                              repository.getCredential().getAdminPassword());
        
        return new BitbucketRemoteClient(authProvider);
    }
    
    public static BitbucketRemoteClient fromHostUrl(String hostUrl)
    {
        AuthProvider authProvider = new NoAuthAuthProvider(hostUrl);
        
        return new BitbucketRemoteClient(authProvider);
    }
}
