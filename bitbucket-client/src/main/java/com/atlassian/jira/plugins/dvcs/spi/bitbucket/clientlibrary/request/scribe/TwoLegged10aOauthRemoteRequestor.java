package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ApiProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import org.apache.http.client.methods.HttpRequestBase;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.util.Map;

/**
 * TwoLegged10aOauthRemoteRequestor
 *
 * Created on 13.7.2012, 10:25:16
 *
 * @author jhocman@atlassian.com
 */
public class TwoLegged10aOauthRemoteRequestor extends ScribeOauthRemoteRequestor
{

	public TwoLegged10aOauthRemoteRequestor(ApiProvider apiProvider, String key, String secret, HttpClientProvider httpClientProvider)
	{
		super(apiProvider, key, secret, httpClientProvider);
	}

	@Override
	protected String afterFinalUriConstructed(HttpRequestBase forMethod, String finalUri, Map<String, ? extends Object> parameters)
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
		log.debug("2LO signing took [{}] ms ", System.currentTimeMillis() - start);

		return finalUri + paramsToString(oauthParams, finalUri.indexOf("?") != -1);
	}

    @Override
	protected boolean isTwoLegged()
	{
	    return true;
	}
}

