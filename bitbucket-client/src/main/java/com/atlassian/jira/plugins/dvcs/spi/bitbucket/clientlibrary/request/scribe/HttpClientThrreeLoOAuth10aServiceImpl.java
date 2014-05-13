package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.HttpClientOauthRequest;
import org.scribe.model.HttpClientOauthResponse;
import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.scribe.utils.MapUtils;

import java.util.Map;

public class HttpClientThrreeLoOAuth10aServiceImpl implements OAuthService
{

    private static final String VERSION = "1.0";

    private OAuthConfig config;
    private DefaultApi10a api;
    private HttpClientProvider httpClientProvider;

    /**
     * Default constructor
     * 
     * @param api OAuth1.0a api information
     * @param config OAuth 1.0a configuration param object
     */
    public HttpClientThrreeLoOAuth10aServiceImpl(DefaultApi10a api, OAuthConfig config, HttpClientProvider httpClientProvider)
    {
      this.api = api;
      this.config = config;
      this.httpClientProvider = httpClientProvider;
    }

    /**
     * {@inheritDoc}
     */
    public Token getRequestToken()
    {
      config.log("obtaining request token from " + api.getRequestTokenEndpoint());
      HttpClientOauthRequest request = new HttpClientOauthRequest(api.getRequestTokenVerb(), api.getRequestTokenEndpoint(), httpClientProvider);

      config.log("setting oauth_callback to " + config.getCallback());
      request.addOAuthParameter(OAuthConstants.CALLBACK, config.getCallback());
      addOAuthParams(request, OAuthConstants.EMPTY_TOKEN);
      appendSignature(request);

      config.log("sending request...");
      HttpClientOauthResponse response = request.sendViaHttpClient();
      String body = response.getContent();

      config.log("response status code: " + response.getStatusCode());
      config.log("response body: " + body);
      return api.getRequestTokenExtractor().extract(body);
    }

    private void addOAuthParams(OAuthRequest request, Token token)
    {
      request.addOAuthParameter(OAuthConstants.TIMESTAMP, api.getTimestampService().getTimestampInSeconds());
      request.addOAuthParameter(OAuthConstants.NONCE, api.getTimestampService().getNonce());
      request.addOAuthParameter(OAuthConstants.CONSUMER_KEY, config.getApiKey());
      request.addOAuthParameter(OAuthConstants.SIGN_METHOD, api.getSignatureService().getSignatureMethod());
      request.addOAuthParameter(OAuthConstants.VERSION, getVersion());
      if(config.hasScope()) request.addOAuthParameter(OAuthConstants.SCOPE, config.getScope());
      request.addOAuthParameter(OAuthConstants.SIGNATURE, getSignature(request, token));

      config.log("appended additional OAuth parameters: " + MapUtils.toString(request.getOauthParameters()));
    }

    /**
     * {@inheritDoc}
     */
    public Token getAccessToken(Token requestToken, Verifier verifier)
    {
      config.log("obtaining access token from " + api.getAccessTokenEndpoint());
      HttpClientOauthRequest request = new HttpClientOauthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint(), httpClientProvider);
      request.addOAuthParameter(OAuthConstants.TOKEN, requestToken.getToken());
      request.addOAuthParameter(OAuthConstants.VERIFIER, verifier.getValue());

      config.log("setting token to: " + requestToken + " and verifier to: " + verifier);
      addOAuthParams(request, requestToken);
      appendSignature(request);
      HttpClientOauthResponse response = request.sendViaHttpClient();
      return api.getAccessTokenExtractor().extract(response.getContent());
    }

    /**
     * {@inheritDoc}
     */
    public void signRequest(Token token, OAuthRequest request)
    {
      config.log("signing request: " + request.getCompleteUrl());
      request.addOAuthParameter(OAuthConstants.TOKEN, token.getToken());

      config.log("setting token to: " + token);
      addOAuthParams(request, token);
      appendSignature(request);
    }

    /**
     * {@inheritDoc}
     */
    public String getVersion()
    {
      return VERSION;
    }

    /**
     * {@inheritDoc}
     */
    public String getAuthorizationUrl(Token requestToken)
    {
      return api.getAuthorizationUrl(requestToken);
    }
    
    private String getSignature(OAuthRequest request, Token token)
    {
      config.log("generating signature...");
      String baseString = api.getBaseStringExtractor().extract(request);
      String signature = api.getSignatureService().getSignature(baseString, config.getApiSecret(), token.getSecret());

      config.log("base string is: " + baseString);
      config.log("signature is: " + signature);
      return signature;
    }

    private void appendSignature(OAuthRequest request)
    {
      switch (config.getSignatureType())
      {
        case Header:
          config.log("using Http Header signature");

          String oauthHeader = api.getHeaderExtractor().extract(request);
          request.addHeader(OAuthConstants.HEADER, oauthHeader);
          break;
        case QueryString:
          config.log("using Querystring signature");

          for (Map.Entry<String, String> entry : request.getOauthParameters().entrySet())
          {
            request.addQuerystringParameter(entry.getKey(), entry.getValue());
          }
          break;
      }
    }
	
}
