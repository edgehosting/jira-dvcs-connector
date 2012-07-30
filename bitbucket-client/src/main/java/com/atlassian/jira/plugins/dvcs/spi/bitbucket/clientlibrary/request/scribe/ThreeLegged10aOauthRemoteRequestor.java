package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

import org.scribe.extractors.HeaderExtractorImpl;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpMethod;

/**
 * ThreeLegged10aOauthRemoteRequestor
 * 
 * 
 * <br />
 * <br />
 * Created on 13.7.2012, 10:26:08 <br />
 * <br />
 * 
 * @author jhocman@atlassian.com
 * 
 */
public class ThreeLegged10aOauthRemoteRequestor extends ScribeOauthRemoteRequestor
{

    private final String accessTokenWithSecret;
    private HeaderExtractorImpl authHeaderCreator;

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
    public ThreeLegged10aOauthRemoteRequestor(String apiUrl, String key, String secret, String accessTokenWithSecret)
    {
        super(apiUrl, key, secret);
        this.accessTokenWithSecret = accessTokenWithSecret;
        this.authHeaderCreator = new HeaderExtractorImpl();
    }

    @Override
    protected void onConnectionCreated(HttpURLConnection connection, HttpMethod method, Map<String, String> parameters)
            throws IOException
    {
        //
        // generate oauth 1.0 params for 3LO - use scribe so far for that ...
        //
        OAuthService service = createOauthService();
        OAuthRequest request = new OAuthRequest(getScribeVerb(method), connection.getURL().toString());
        
        addParametersForSigning(request, parameters);
        
        service.signRequest(generateAccessTokenObject(accessTokenWithSecret), request);

        String header = authHeaderCreator.extract(request);
        connection.setRequestProperty(OAuthConstants.HEADER, header);

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

    @Override
    protected boolean isTwoLegged()
    {
        return false;
    }

}
