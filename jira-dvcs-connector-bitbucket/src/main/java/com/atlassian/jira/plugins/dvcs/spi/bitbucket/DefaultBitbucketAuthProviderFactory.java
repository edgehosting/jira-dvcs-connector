package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.NoAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe.ThreeLegged10aOauthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe.TwoLeggedOauthProvider;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.plugin.PluginAccessor;

/**
 * BitbucketAuthProviderFactory
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public final class DefaultBitbucketAuthProviderFactory implements BitbuckeAuthProviderFactory
{
    private final Encryptor encryptor;
    private final String userAgent;

    public DefaultBitbucketAuthProviderFactory(Encryptor encryptor,
            PluginAccessor pluginAccessor)
    {
        this.encryptor = encryptor;
        this.userAgent = DvcsConstants.getUserAgent(pluginAccessor);
    }

    @Override
    public AuthProvider getForOrganization(Organization organization)
    {
        return createProvider(organization.getHostUrl(), organization.getName(), organization.getCredential());
    }

    @Override
    public AuthProvider getForRepository(Repository repository)
    {
        return createProvider(repository.getOrgHostUrl(), repository.getOrgName(), repository.getCredential());
    }

    @Override
    public AuthProvider getForRepository(Repository repository, int apiVersion)
    {
        AuthProvider authProvider = createProvider(repository.getOrgHostUrl(), repository.getOrgName(), repository.getCredential());
        authProvider.setApiVersion(apiVersion);
        return authProvider;
    }

    @Override
    public AuthProvider getNoAuthClient(String hostUrl)
    {
        AuthProvider authProvider = new NoAuthAuthProvider(hostUrl);
        authProvider.setUserAgent(userAgent);
        return authProvider;
    }

    /**
     * @param hostUrl
     * @param name
     * @param credential
     * @return
     */
    @Override
    public AuthProvider createProvider(String hostUrl, String name, Credential credential)
    {
        String username =  credential.getAdminUsername();
        String password = credential.getAdminPassword();
        String key = credential.getOauthKey();
        String secret = credential.getOauthSecret();
        String accessToken = credential.getAccessToken();

        AuthProvider oAuthProvider;
        // 3LO (key, secret, token)
        if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(secret) && StringUtils.isNotBlank(accessToken))
        {
            oAuthProvider = new ThreeLegged10aOauthProvider(hostUrl, key, secret, accessToken);
        }

        // 2LO (key, secret)
        else if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(secret))
        {
            oAuthProvider = new TwoLeggedOauthProvider(hostUrl, key, secret);
        }

        // basic (username, password)
        else if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
        {
            String decryptedPassword = encryptor.decrypt(password, name, hostUrl);
            oAuthProvider =  new BasicAuthAuthProvider(hostUrl, username,decryptedPassword);
        } else
        {
            oAuthProvider = new NoAuthAuthProvider(hostUrl);
        }

        oAuthProvider.setUserAgent(userAgent);
        return oAuthProvider;
    }
}
