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
	
	private int apiVersion = 1;
	
	public AbstractOauthProvider(String hostUrl)
	{
		this.hostUrl = hostUrl;
	}

    @Override
	public String getApiUrl()
	{
		return hostUrl.replaceAll("/$", "") + "/api/"  + apiVersion + ".0";
	}

    public void setApiVersion(int apiVersion)
    {
        this.apiVersion = apiVersion;
    }

}

