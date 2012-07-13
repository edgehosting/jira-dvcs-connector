package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

/**
 * 
 * BaseOauthProvider
 *
 * 
 * <br /><br />
 * Created on 13.7.2012, 17:00:59
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public abstract class AbstractOauthProvider implements AuthProvider
{

	private final String hostUrl;
	
	public AbstractOauthProvider(String hostUrl)
	{
		super();
		this.hostUrl = hostUrl;
	}

	@Override
	public AuthKind getKind()
	{
		return AuthKind.THREE_LEGGED_OAUTH_10a;
	}

	@Override
	public String getApiUrl()
	{
		if (hostUrl.endsWith("/")) {
			return hostUrl + "api/1.0";
		} else {
			return hostUrl + "/api/1.0";
		}
	}

}

