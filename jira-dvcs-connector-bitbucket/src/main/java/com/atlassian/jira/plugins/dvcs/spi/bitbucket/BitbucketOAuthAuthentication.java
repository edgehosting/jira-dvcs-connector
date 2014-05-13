package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.sal.api.net.Request;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.lang.StringUtils;
import org.scribe.model.Token;

public class BitbucketOAuthAuthentication implements Authentication
{

	private final String accessToken;

	public BitbucketOAuthAuthentication(String accessToken)
	{
		this.accessToken = accessToken;
	}

	public String getAccessToken()
	{
		return accessToken;
	}

	public static String generateAccessTokenString(Token accessTokenObj)
	{
		return accessTokenObj.getToken() + "&" + accessTokenObj.getSecret();
	}

	public static Token generateAccessTokenObject(String accessTokenStr)
	{
		if (!StringUtils.isBlank(accessTokenStr))
		{
			String[] parts = accessTokenStr.split("&");
			if (parts.length == 2)
			{
				return new Token(parts[0], parts[1]);
			}
		}
		return null;
	}

	@Override
	public void addAuthentication(Request<?, ?> request, String url)
	{
		request.addHeader("Authorization", "token " + accessToken);
	}

	@Override
	public void addAuthentication(HttpMethod forMethod, HttpClient forClient)
	{
		forMethod.addRequestHeader("Authorization", "token " + accessToken);
	}
}
