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
public abstract class AbstractAuthProvider implements AuthProvider
{
	private final String hostUrl;
	
	public AbstractAuthProvider(String hostUrl)
	{
		this.hostUrl = hostUrl;
	}

    @Override
	public String getApiUrl()
	{
		return hostUrl.replaceAll("/$", "") + "/api/1.0";
	}

}

