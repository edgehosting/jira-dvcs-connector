package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.NoAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe.ThreeLegged10aOauthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe.TwoLeggedOauthProvider;

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
        // no auth
        AuthProvider authProvider = new NoAuthAuthProvider(organization.getHostUrl());
       
        // basic
        if (StringUtils.isNotBlank(organization.getCredential().getAdminUsername()))
        {
            authProvider = new BasicAuthAuthProvider(organization.getHostUrl(), organization.getCredential()
                    .getAdminUsername(), organization.getCredential().getAdminPassword());
        } 
        
        // oauth 3LO - standard (10a)
        else if (StringUtils.isNotBlank( organization.getCredential().getAccessToken()) )
        {
            authProvider = new ThreeLegged10aOauthProvider(organization.getHostUrl(), oauth.getClientId(),
                    oauth.getClientSecret(), organization.getCredential().getAccessToken());
        }
        
        // oauth 2LO
        else if (StringUtils.isNotBlank(organization.getCredential().getOauthKey())
                && StringUtils.isNotBlank(organization.getCredential().getOauthSecret()))
        {
            authProvider = new TwoLeggedOauthProvider(organization.getHostUrl(), organization.getCredential()
                    .getOauthKey(), organization.getCredential().getOauthSecret());
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
        
        else if (StringUtils.isNotBlank(repository.getCredential().getOauthKey())
                && StringUtils.isNotBlank(repository.getCredential().getOauthSecret()))
        {
            authProvider = new TwoLeggedOauthProvider(repository.getOrgHostUrl(), repository.getCredential()
                    .getOauthKey(), repository.getCredential().getOauthSecret());
        }

        return authProvider;
    }
}
