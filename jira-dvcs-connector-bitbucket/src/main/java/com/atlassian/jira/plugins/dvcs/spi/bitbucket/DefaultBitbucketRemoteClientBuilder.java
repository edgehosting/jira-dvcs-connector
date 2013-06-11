package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.NoAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe.ThreeLegged10aOauthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe.TwoLeggedOauthProvider;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.plugin.PluginAccessor;

public class DefaultBitbucketRemoteClientBuilder implements BitbucketClientBuilder
{
    private AuthProvider authProvider;
    private int apiVersion = 1;
    private boolean cached;
    private int timeout = -1;

    private final Encryptor encryptor;
    private final String userAgent;

    public DefaultBitbucketRemoteClientBuilder(Encryptor encryptor,
            PluginAccessor pluginAccessor)
    {
        this.encryptor = encryptor;
        this.userAgent = DvcsConstants.getUserAgent(pluginAccessor);
    }

    @Override
    public BitbucketClientBuilder forOrganization(Organization organization)
    {
        this.authProvider = createProvider(organization.getHostUrl(), organization.getName(), organization.getCredential());
        return this;
    }

    @Override
    public BitbucketClientBuilder forRepository(Repository repository)
    {
        this.authProvider = createProvider(repository.getOrgHostUrl(), repository.getOrgName(), repository.getCredential());
        return this;
    }

    @Override
    public BitbucketClientBuilder noAuthClient(String hostUrl)
    {
        this.authProvider = new NoAuthAuthProvider(hostUrl);
        this.authProvider.setUserAgent(userAgent);
        return this;
    }

    @Override
    public BitbucketClientBuilder authClient(String hostUrl, String name, Credential credential)
    {
        this.authProvider = createProvider(hostUrl, name, credential);
        return this;
    }

    @Override
    public BitbucketClientBuilder cached()
    {
        this.cached = true;
        return this;
    }

    @Override
    public BitbucketClientBuilder apiVersion(int apiVersion)
    {
        this.apiVersion = apiVersion;
        return this;
    }

    @Override
    public BitbucketClientBuilder timeout(int timeout)
    {
        this.timeout = timeout;
        return this;
    }

    @Override
    public BitbucketRemoteClient build()
    {
        if (authProvider == null)
        {
            throw new IllegalStateException("AuthProvider must be provided.");
        }

        authProvider.setApiVersion(apiVersion);

        authProvider.setCached(cached);

        authProvider.setTimeout(timeout);

        return new BitbucketRemoteClient(authProvider);
    }


    private AuthProvider createProvider(String hostUrl, String name, Credential credential)
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
