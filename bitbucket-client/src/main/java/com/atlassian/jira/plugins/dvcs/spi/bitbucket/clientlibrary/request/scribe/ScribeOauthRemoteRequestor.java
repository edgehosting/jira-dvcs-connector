package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import java.util.Map;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BaseRemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpMethod;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.DebugOutputStream;

/**
 * ScribeOauthRemoteRequestor
 * 
 * 
 * <br />
 * <br />
 * Created on 13.7.2012, 10:25:16 <br />
 * <br />
 * 
 * @author jhocman@atlassian.com
 * 
 */
public abstract class ScribeOauthRemoteRequestor extends BaseRemoteRequestor
{

    protected static Logger log = LoggerFactory.getLogger(ScribeOauthRemoteRequestor.class);

    protected final String key;

    protected final String secret;

    public ScribeOauthRemoteRequestor(String apiUrl, String key, String secret)
    {
        super(apiUrl);
        this.key = key;
        this.secret = secret;
    }

    protected OAuthService createOauthService()
    {
        return new ServiceBuilder().provider(new OAuthBitbucket10aApi(apiUrl, isTwoLegged())).apiKey(key)
                .signatureType(SignatureType.Header).apiSecret(secret).debugStream(new DebugOutputStream(log)).build();
    }
    
    protected void addParametersForSigning(OAuthRequest request, Map<String, String> parameters)
    {
        
        if (parameters == null) {
            return;
        }
        
        Verb method = request.getVerb();
        
        if (method == Verb.POST || method == Verb.PUT) {
            
            for (String paramName : parameters.keySet())
            {
                request.addBodyParameter(paramName, parameters.get(paramName));
            }
            
        }
        
    }

    protected abstract boolean isTwoLegged();
    
    protected Verb getScribeVerb(HttpMethod forMethod)
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
            super("", "");
        }
    }

}
