package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.util.HashMap;
import java.util.Map;

/**
 * ThreeLeggedOauthRemoteRequestor
 *
 * 
 * <br /><br />
 * Created on 13.7.2012, 10:26:08
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class ThreeLeggedOauthRemoteRequestor extends BaseRemoteRequestor
{
	private final String accessToken;

	public ThreeLeggedOauthRemoteRequestor(String apiUrl, String accessToken)
	{
		super(apiUrl);
		this.accessToken = accessToken;
	}
	
	@Override
	protected String afterFinalUriConstructed(HttpMethod forMethod, String finalUri, Map<String, String> parameters)
	{
		Map<String, String> oauthParams = new HashMap<String, String>();
		oauthParams.put("access_token", accessToken);
		
		return finalUri + paramsToString(oauthParams, finalUri.contains("?"));
	}
}

