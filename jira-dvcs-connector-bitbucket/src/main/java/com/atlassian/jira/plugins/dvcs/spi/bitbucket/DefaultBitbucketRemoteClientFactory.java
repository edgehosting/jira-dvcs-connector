package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.NoAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe.ThreeLegged10aOauthProvider;

/**
 * BitbucketRemoteClientFactory
 * 
 * @author Martin Skurla mskurla@atlassian.com
 */
public final class DefaultBitbucketRemoteClientFactory implements BitbucketClientRemoteFactory
{
    private final BitbucketOAuth oauth;

    public DefaultBitbucketRemoteClientFactory(BitbucketOAuth oauth)
    {
        this.oauth = oauth;
    }

    @Override
    public BitbucketRemoteClient getForOrganization(Organization organization)
    {
        AuthProvider authProvider = createProviderForOrganization(organization);

        return new BitbucketRemoteClient(authProvider);
    }

    @Override
    public BitbucketRemoteClient getForRepository(Repository repository)
    {
        AuthProvider authProvider = createProviderForRepository(repository);

        return new BitbucketRemoteClient(authProvider);
    }
    
    @Override
    public BitbucketRemoteClient getNoAuthClient(String hostUrl)
    {
        AuthProvider authProvider = new NoAuthAuthProvider(hostUrl);

        return new BitbucketRemoteClient(authProvider);
    }
    

    private AuthProvider createProviderForOrganization(Organization organization)
    {
        AuthProvider authProvider = new NoAuthAuthProvider(organization.getHostUrl());
       
        if (StringUtils.isNotBlank(organization.getCredential().getAdminUsername()))
        {
            authProvider = new BasicAuthAuthProvider(organization.getHostUrl(), organization.getCredential()
                    .getAdminUsername(), organization.getCredential().getAdminPassword());
        } 
        
        else if (StringUtils.isNotBlank( organization.getCredential().getAccessToken()) )
        {
            authProvider = new ThreeLegged10aOauthProvider(organization.getHostUrl(), oauth.getClientId(),
                    oauth.getClientSecret(), organization.getCredential().getAccessToken());
        }

        return authProvider;
    }
    
    private AuthProvider createProviderForRepository(Repository repository)
    {
        AuthProvider authProvider = new NoAuthAuthProvider(repository.getOrgHostUrl());
        
        if (StringUtils.isNotBlank(repository.getCredential().getAdminUsername()))
        {
            authProvider = new BasicAuthAuthProvider(repository.getOrgHostUrl(), repository.getCredential()
                    .getAdminUsername(), repository.getCredential().getAdminPassword());
        }
        
        else  if (StringUtils.isNotBlank( repository.getCredential().getAccessToken()) )
        {
            authProvider = new ThreeLegged10aOauthProvider(repository.getOrgHostUrl(), oauth.getClientId(),
                    oauth.getClientSecret(), repository.getCredential().getAccessToken());
        } 

        return authProvider;
    }
}
