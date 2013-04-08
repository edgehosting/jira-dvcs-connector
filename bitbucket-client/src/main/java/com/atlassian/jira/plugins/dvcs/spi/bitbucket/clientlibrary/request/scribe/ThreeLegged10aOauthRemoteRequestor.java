package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.scribe.extractors.HeaderExtractorImpl;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ApiProvider;

/**
 * ThreeLegged10aOauthRemoteRequestor
 *
 * Created on 13.7.2012, 10:26:08 <br />
 *
 * @author jhocman@atlassian.com
 */
public class ThreeLegged10aOauthRemoteRequestor extends ScribeOauthRemoteRequestor
{
    private final String accessTokenWithSecret;
    private final HeaderExtractorImpl authHeaderCreator;

    /**
     * The Constructor.
     *
     * @param apiUrl
     *            the api url
     * @param key
     *            the key
     * @param secret
     *            the secret
     * @param accessTokenWithSecret
     *            the access token = TOKEN + '&' + TOKEN_SECRET
     */
    public ThreeLegged10aOauthRemoteRequestor(ApiProvider apiProvider, String key, String secret, String accessTokenWithSecret)
    {
        super(apiProvider, key, secret);
        this.accessTokenWithSecret = accessTokenWithSecret;
        this.authHeaderCreator = new HeaderExtractorImpl();
    }


    @Override
    protected void onConnectionCreated(DefaultHttpClient client, HttpRequestBase method, Map<String, String> parameters)
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
