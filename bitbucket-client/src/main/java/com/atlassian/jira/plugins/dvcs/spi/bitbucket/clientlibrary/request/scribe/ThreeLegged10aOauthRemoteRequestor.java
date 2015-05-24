package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ApiProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.scribe.extractors.HeaderExtractorImpl;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.io.IOException;
import java.util.Map;

/**
 * ThreeLegged10aOauthRemoteRequestor
 *
 * @author jhocman@atlassian.com
 */
public class ThreeLegged10aOauthRemoteRequestor extends ScribeOauthRemoteRequestor
{
    private final String accessTokenWithSecret;
    private final HeaderExtractorImpl authHeaderCreator;

    /**
    /**
     *
     * @param apiProvider
     * @param key
     * @param secret
     * @param accessTokenWithSecret the access token = TOKEN + '&amp;' + TOKEN_SECRET
     * @param httpClientProvider
     */
    public ThreeLegged10aOauthRemoteRequestor(ApiProvider apiProvider, String key, String secret, String accessTokenWithSecret, HttpClientProvider httpClientProvider)
    {
        super(apiProvider, key, secret, httpClientProvider);
        this.accessTokenWithSecret = accessTokenWithSecret;
        this.authHeaderCreator = new HeaderExtractorImpl();
    }


    @Override
    protected void onConnectionCreated(HttpClient client, HttpRequestBase method, Map<String, ? extends Object> parameters)
            throws IOException
    {
        long start = System.currentTimeMillis();
        //
        // generate oauth 1.0 params for 3LO - use scribe so far for that ...
        //
        OAuthService service = createOauthService();
        OAuthRequest request = new OAuthRequest(Verb.valueOf(method.getMethod()), method.getURI().toString());

        addParametersForSigning(request, parameters);

        service.signRequest(generateAccessTokenObject(accessTokenWithSecret), request);

        String header = authHeaderCreator.extract(request);
        method.addHeader(OAuthConstants.HEADER, header);

        log.debug("3LO signing took [{}] ms ", System.currentTimeMillis() - start);
    }

    public static Token generateAccessTokenObject(String accessToken)
    {
        if (StringUtils.isNotBlank(accessToken))
        {
            String[] parts = accessToken.split("&");
            if (parts.length == 2)
            {
                return new Token(parts[0], parts[1]);
            }
        }
        return null;
    }

    @Override
    protected boolean isTwoLegged()
    {
        return false;
    }
}
