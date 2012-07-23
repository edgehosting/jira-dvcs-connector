package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import java.util.Map;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpMethod;

/**
 * ThreeLegged10aOauthRemoteRequestor
 *
 * 
 * <br /><br />
 * Created on 13.7.2012, 10:26:08
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class ThreeLegged10aOauthRemoteRequestor extends ScribeOauthRemoteRequestor
{

	private final String accessTokenWithSecret;

	/**
	 * The Constructor.
	 *
	 * @param apiUrl the api url
	 * @param key the key
	 * @param secret the secret
	 * @param accessTokenWithSecret the access token = TOKEN + '&' + TOKEN_SECRET
	 */
	public ThreeLegged10aOauthRemoteRequestor(String apiUrl, String key, String secret, String accessTokenWithSecret)
	{
		super(apiUrl, key, secret);
		this.accessTokenWithSecret = accessTokenWithSecret;
	}

	@Override
	protected String afterFinalUriConstructed(HttpMethod forMethod, String finalUri)
	{
	     //
        // generate oauth 1.0 params for 3LO - use scribe so far for that ...
	    OAuthService service = createOauthService();
        OAuthRequest request = new OAuthRequest(getScribeVerb(forMethod), finalUri);
        service.signRequest(generateAccessTokenObject(accessTokenWithSecret), request);
        Map<String, String> oauthParams = request.getOauthParameters();
        //
        //
        //
		return finalUri + paramsToString(oauthParams, finalUri.contains("?"));
	}

	public static Token generateAccessTokenObject(String accessToken)
    {
        if (accessToken != null && !accessToken.trim().isEmpty())
        {
            String[] parts = accessToken.split("&");
            if (parts.length == 2)
            {
                return new Token(parts[0], parts[1]);
            }
        }
        return null;
    }
}

