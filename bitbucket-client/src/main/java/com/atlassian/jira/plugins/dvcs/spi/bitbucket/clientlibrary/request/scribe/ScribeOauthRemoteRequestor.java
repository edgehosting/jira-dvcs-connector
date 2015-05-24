package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.scribe;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ApiProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BaseRemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.DebugOutputStream;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class ScribeOauthRemoteRequestor extends BaseRemoteRequestor
{
    protected static Logger log = LoggerFactory.getLogger(ScribeOauthRemoteRequestor.class);
    protected final String key;
    protected final String secret;

    public ScribeOauthRemoteRequestor(ApiProvider apiProvider, String key, String secret, HttpClientProvider httpClientProvider)
    {
        super(apiProvider, httpClientProvider);
        this.key = key;
        this.secret = secret;
    }

    protected OAuthService createOauthService()
    {
        return new ServiceBuilder().provider(new OAuthBitbucket10aApi(apiProvider.getApiUrl(), isTwoLegged())).apiKey(key)
                .signatureType(SignatureType.Header).apiSecret(secret).debugStream(new DebugOutputStream(log)).build();
    }

    protected void addParametersForSigning(final OAuthRequest request, final Map<String, ? extends Object> parameters)
    {
        if (parameters == null)
        {
            return;
        }
        Verb method = request.getVerb();
        if (method == Verb.POST || method == Verb.PUT)
        {
            processParams(parameters, new ParameterProcessor()
            {
                @Override
                public void process(String key, String value)
                {
                    request.addBodyParameter(key, value);
                }

            });
        }
    }

    protected abstract boolean isTwoLegged();

    static class EmptyToken extends Token
    {
        private static final long serialVersionUID = -3452471071058444368L;
        public EmptyToken()
        {
            super("", "");
        }
    }

}
