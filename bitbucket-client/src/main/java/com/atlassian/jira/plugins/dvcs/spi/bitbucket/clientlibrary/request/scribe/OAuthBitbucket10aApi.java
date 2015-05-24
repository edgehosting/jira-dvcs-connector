package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;
import org.scribe.services.TimestampService;
import org.scribe.services.TimestampServiceImpl;

import java.util.UUID;

public class OAuthBitbucket10aApi extends DefaultApi10a
{

    private final String apiUrl;
    private final boolean isTwoLegged;
    
    /**
     * Customized {@link TimestampService#getNonce()} to be more unique. Our implementation executes too many signed requests for full
     * synchronization, which is resulting into the nonce collision.
     */
    private final TimestampService timestampService = new TimestampServiceImpl()
    {

        @Override
        public String getNonce()
        {
            return UUID.randomUUID().toString();
        }

    };

    /**
     * The Constructor.
     * 
     * @param apiUrl
     *            the api url
     * @param isTwoLegged
     *            <code>true</code> if we should use 2LO mechanism,
     *            <code>false</code> for 3LO
     */
    public OAuthBitbucket10aApi(String apiUrl, boolean isTwoLegged)
    {
        this.apiUrl = apiUrl;
        this.isTwoLegged = isTwoLegged;
    }
    
    @Override
    public TimestampService getTimestampService()
    {
        return timestampService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestTokenEndpoint()
    {
        return apiUrl + "/oauth/request_token/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAccessTokenEndpoint()
    {
        return apiUrl + "/oauth/access_token/";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthorizationUrl(Token requestToken)
    {
        return String.format(apiUrl + "/oauth/authenticate/?oauth_token=%s", requestToken.getToken());
    }

    @Override
    public OAuthService createService(OAuthConfig config)
    {
        if (isTwoLegged)
        {
            return new TwoLoOAuth10aServiceImpl(this, config);
        } else
        {
            return super.createService(config);
        }

    }

}
