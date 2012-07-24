package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import java.util.Map;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BaseRemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpMethod;

/**
 * TwoLeggedOauthRemoteRequestor
 *
 * 
 * <br /><br />
 * Created on 13.7.2012, 10:25:16
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class TwoLeggedOauthRemoteRequestor extends BaseRemoteRequestor
{

	private final String key;
	
	private final String secret;

	public TwoLeggedOauthRemoteRequestor(String apiUrl, String key, String secret)
	{
		super(apiUrl);
		this.key = key;
		this.secret = secret;
	}

	@Override
	protected String afterFinalUriConstructed(HttpMethod forMethod, String finalUri)
	{
		//
		// generate oauth 1.0 params for 2LO - use scribe so far for that ...
		//
		OAuthService service = new ServiceBuilder().provider(new TwoLeggedOAuthBitbucket10aApi(apiUrl)).apiKey(key)
				.signatureType(SignatureType.Header).apiSecret(secret).build();
		OAuthRequest request = new OAuthRequest(getScribeVerb(forMethod), finalUri);
		service.signRequest(new EmptyToken(), request);
		Map<String, String> oauthParams = request.getOauthParameters();
		//
		//
		//
		
		return finalUri + paramsToString(oauthParams, finalUri.indexOf("?") != -1);
	}

	private Verb getScribeVerb(HttpMethod forMethod)
	{
		switch (forMethod)
		{
		case PUT:
			return Verb.PUT;
		case DELETE:
			return Verb.DELETE;
		case POST:
			return Verb.POST;
		default:
			return Verb.GET;
		}
	}
	
	static class EmptyToken extends Token 
    {
		private static final long serialVersionUID = -3452471071058444368L;
		public EmptyToken()
		{
			super("","");
		}
	}
	
}

