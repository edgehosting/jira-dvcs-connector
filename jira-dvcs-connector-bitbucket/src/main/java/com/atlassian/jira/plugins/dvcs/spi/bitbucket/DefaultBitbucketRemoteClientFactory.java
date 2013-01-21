package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
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
    private final Encryptor encryptor;

    public DefaultBitbucketRemoteClientFactory(BitbucketOAuth oauth, Encryptor encryptor)
    {
        this.oauth = oauth;
        this.encryptor = encryptor;
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
    public BitbucketRemoteClient getForRepository(Repository repository, int apiVersion)
    {
        AuthProvider authProvider = createProviderForRepository(repository);
        authProvider.setApiVersion(apiVersion);

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
            String decryptedPassword = encryptor.decrypt(organization.getCredential().getAdminPassword(),
                                                         organization.getName(),
                                                         organization.getHostUrl());
            
            authProvider = new BasicAuthAuthProvider(organization.getHostUrl(),
                                                     organization.getCredential().getAdminUsername(),
                                                     decryptedPassword);
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
            String decryptedPassword = encryptor.decrypt(repository.getCredential().getAdminPassword(),
                                                         repository.getOrgName(),
                                                         repository.getOrgHostUrl());
            
            authProvider = new BasicAuthAuthProvider(repository.getOrgHostUrl(),
                                                     repository.getCredential().getAdminUsername(),
                                                     decryptedPassword);
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
