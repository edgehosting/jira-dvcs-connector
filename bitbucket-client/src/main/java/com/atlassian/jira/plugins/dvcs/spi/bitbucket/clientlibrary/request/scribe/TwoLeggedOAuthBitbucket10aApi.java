package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.OAuthConfig;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

/**
 * TwoLeggedOAuthBitbucket10aApi
 * 
 * <br />
 * <br />
 * Created on 14.6.2012, 13:40:17 <br />
 * <br />
 * 
 * @author jhocman@atlassian.com
 * 
 */
public class TwoLeggedOAuthBitbucket10aApi extends DefaultApi10a
{

	private final String apiUrl;

	public TwoLeggedOAuthBitbucket10aApi(String apiUrl)
	{
		this.apiUrl = apiUrl;
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
		return String.format(apiUrl + "/oauth/authenticate/?oauth_token=%s",
				requestToken.getToken());
	}
	
	@Override
	public OAuthService createService(OAuthConfig config)
	{
		return new TwoLoOAuth10aServiceImpl(this, config);
	}

}
