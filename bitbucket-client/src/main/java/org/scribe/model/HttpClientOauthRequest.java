package org.scribe.model;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.scribe.exceptions.OAuthConnectionException;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProxyConfig;

public class HttpClientOauthRequest extends OAuthRequest
{
    private HttpClientProxyConfig proxyConfig = new HttpClientProxyConfig();
    
    public HttpClientOauthRequest(Verb verb, String url)
    {
        super(verb, url);
    }
    
    public HttpClientOauthResponse sendViaHttpClient()
    {
      try
      {
        return sendInternal();
      }
      catch (Exception e)
      {
        throw new OAuthConnectionException(e);
      }
    }

    private HttpClientOauthResponse sendInternal() throws Exception
    {
        String urlToGo = getCompleteUrl();

        DefaultHttpClient client = new DefaultHttpClient();
        proxyConfig.configureProxy(client, urlToGo);
        
        // always POST when doing oauth dance on bitbucket
        // see DefaultAp10Api#getAccessTokenVerb or getRequestTokenVerb
        HttpRequestBase requestMethod = new HttpPost();
        requestMethod.setURI(new URI(urlToGo));
        
        setHeaders(requestMethod);
        setPayloadParams((HttpEntityEnclosingRequestBase) requestMethod);
        
        HttpResponse response = client.execute(requestMethod);
        
        return new HttpClientOauthResponse(response);
    }
    
    private void setHeaders(HttpRequestBase requestMethod)
    {
        if (getHeaders() != null) {
            for (String key : getHeaders().keySet())
            {
                requestMethod.setHeader(key, getHeaders().get(key));
            }
        }
    }

    private void setPayloadParams(HttpEntityEnclosingRequestBase method) throws IOException
    {
        if (getBodyParams() != null)
        {
            StringEntity entity = new StringEntity(getBodyParams().asFormUrlEncodedString());
            entity.setContentType("application/x-www-form-urlencoded");
            method.setEntity(entity);
        }

    }


}

