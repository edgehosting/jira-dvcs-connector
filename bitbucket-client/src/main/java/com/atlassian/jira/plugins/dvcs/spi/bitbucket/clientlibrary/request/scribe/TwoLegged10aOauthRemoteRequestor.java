package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import java.util.Map;

import org.apache.http.client.methods.HttpRequestBase;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ApiProvider;

/**
 * TwoLegged10aOauthRemoteRequestor
 *
 * 
 * <br /><br />
 * Created on 13.7.2012, 10:25:16
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class TwoLegged10aOauthRemoteRequestor extends ScribeOauthRemoteRequestor
{

	public TwoLegged10aOauthRemoteRequestor(ApiProvider apiProvider, String key, String secret)
	{
		super(apiProvider, key, secret);
	}

	@Override
	protected String afterFinalUriConstructed(HttpRequestBase forMethod, String finalUri, Map<String, String> parameters)
	{
	    long start = System.currentTimeMillis();
		//
		// generate oauth 1.0 params for 2LO - use scribe so far for that ...
		//
		OAuthService service = createOauthService();
		OAuthRequest request = new OAuthRequest(Verb.valueOf(forMethod.getMethod()), finalUri);
		
		addParametersForSigning(request, parameters);
		
		service.signRequest(new EmptyToken(), request);
		Map<String, String> oauthParams = request.getOauthParameters();
		//
		//
		//
		log.debug("2LO signing took [{}] ms ", System.currentTimeMillis() - start);
		
		return finalUri + paramsToString(oauthParams, finalUri.indexOf("?") != -1);
	}


    @Override
	protected boolean isTwoLegged()
	{
	    return true;
	}
}

