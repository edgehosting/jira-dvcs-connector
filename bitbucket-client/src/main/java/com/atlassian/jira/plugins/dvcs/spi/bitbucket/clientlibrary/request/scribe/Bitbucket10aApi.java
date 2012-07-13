package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

/**
 * Bitbucket10aApi
 * TODO it is staging for testing
 * 
 * <br />
 * <br />
 * Created on 14.6.2012, 13:40:17 <br />
 * <br />
 * 
 * @author jhocman@atlassian.com
 * 
 */
public class Bitbucket10aApi extends DefaultApi10a
{

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getRequestTokenEndpoint()
	{
		return "https://staging.bitbucket.org/api/1.0/oauth/request_token/";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAccessTokenEndpoint()
	{
		return "https://staging.bitbucket.org/api/1.0/oauth/access_token/";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAuthorizationUrl(Token requestToken)
	{
		return String.format("https://staging.bitbucket.org/api/1.0/oauth/authenticate/?oauth_token=%s",
				requestToken.getToken());
	}
	
	@Override
	public OAuthService createService(OAuthConfig config)
	{
		return new TwoLoOAuth10aServiceImpl(this, config);
	}

}
