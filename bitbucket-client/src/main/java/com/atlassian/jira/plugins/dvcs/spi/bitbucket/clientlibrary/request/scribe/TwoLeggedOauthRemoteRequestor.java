package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import java.util.Map;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BaseRemoteRequestor;

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
	protected String afterFinalUriConstructed(String finalUri)
	{
		//
		// generate oauth 1.0 params for 2LO - use scribe so far for that ...
		//
		OAuthService service = new ServiceBuilder().provider(new Bitbucket10aApi(apiUrl)).apiKey(key)
				.signatureType(SignatureType.Header).apiSecret(secret).build();
		OAuthRequest request = new OAuthRequest(Verb.GET, finalUri);
		service.signRequest(new EmptyToken(), request);
		Map<String, String> oauthParams = request.getOauthParameters();
		//
		//
		//
		
		return finalUri + paramsToString(oauthParams, finalUri.indexOf("?") != -1);
	}
	
	static class EmptyToken extends Token {
		private static final long serialVersionUID = -3452471071058444368L;
		public EmptyToken()
		{
			super("","");
		}
	}
	
}

