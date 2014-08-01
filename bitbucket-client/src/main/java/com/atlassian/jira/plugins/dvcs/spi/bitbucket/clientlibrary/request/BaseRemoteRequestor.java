package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BadRequestRetryer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.SystemUtils;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;

/**
 * BaseRemoteRequestor
 * <p/>
 * <p/>
 * <br />
 * <br />
 * Created on 13.7.2012, 10:25:24 <br />
 * <br />
 *
 * @author jhocman@atlassian.com
 */
public class BaseRemoteRequestor implements RemoteRequestor
{
    private final Logger log = LoggerFactory.getLogger(BaseRemoteRequestor.class);

    protected final ApiProvider apiProvider;

    private final HttpClientProvider httpClientProvider;

    public BaseRemoteRequestor(ApiProvider apiProvider, HttpClientProvider httpClientProvider)
    {
        this.apiProvider = apiProvider;
        this.httpClientProvider = httpClientProvider;
    }

    @Override
    public <T> T get(String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        return getWithRetry(uri, parametersToListParams(parameters), callback);
    }

    private Map<String, List<String>> parametersToListParams(Map<String, String> parameters)
    {
        if (parameters == null)
        {
            return null;
        } else
        {
            return Maps.transformValues(parameters, STRING_TO_LIST_STRING);
        }
    }

    @Override
    public <T> T getWithMultipleVals(String uri, Map<String, List<String>> parameters, ResponseCallback<T> callback)
    {
        return getWithRetry(uri, parameters, callback);
    }

    @Override
    public <T> T delete(String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        return deleteWithRetry(uri, parametersToListParams(parameters), callback);
    }

    @Override
    public <T> T post(String uri, Map<String, ? extends Object> parameters, ResponseCallback<T> callback)
    {
        return postWithRetry(uri, parameters, callback);
    }

    @Override
    public <T> T post(final String uri, final String body, final ContentType contentType, final ResponseCallback<T> callback)
    {
        HttpPost method = new HttpPost();
        return requestWithBody(method, uri, body, contentType, callback);
    }

    @Override
    public <T> T put(String uri, Map<String, String> parameters, ResponseCallback<T> callback)
    {
        return putWithRetry(uri, parameters, callback);
    }

    @Override
    public <T> T put(String uri, String body, ContentType contentType, ResponseCallback<T> callback)
    {
        HttpPut method = new HttpPut();
        return requestWithBody(method, uri, body, contentType, callback);
    }

    // --------------------------------------------------------------------------------------------------
    // Retryers...
    // --------------------------------------------------------------------------------------------------

    private <T> T getWithRetry(final String uri, final Map<String, List<String>> parameters,
                               final ResponseCallback<T> callback)
    {
        return new BadRequestRetryer<T>().retry(new Callable<T>()
        {
            @Override
            public T call() throws Exception
            {
                HttpGet getMethod = new HttpGet();
                return requestWithoutPayload(getMethod, uri, parameters, callback);
            }
        });
    }

    private <T> T deleteWithRetry(final String uri, final Map<String, List<String>> parameters,
                                  final ResponseCallback<T> callback)
    {
        return new BadRequestRetryer<T>().retry(new Callable<T>()
        {
            @Override
            public T call() throws Exception
            {
                HttpDelete method = new HttpDelete();
                return requestWithoutPayload(method, uri, parameters, callback);
            }
        });
    }

    private <T> T postWithRetry(final String uri, final Map<String, ? extends Object> parameters,
                                final ResponseCallback<T> callback)
    {
        return new BadRequestRetryer<T>().retry(new Callable<T>()
        {
            @Override
            public T call() throws Exception
            {
                HttpPost method = new HttpPost();
                return requestWithPayload(method, uri, parameters, callback);
            }
        });
    }

    private <T> T putWithRetry(final String uri, final Map<String, String> parameters,
                               final ResponseCallback<T> callback)
    {
        return new BadRequestRetryer<T>().retry(new Callable<T>()
        {
            @Override
            public T call() throws Exception
            {
                HttpPut method = new HttpPut();
                return requestWithPayload(method, uri, parameters, callback);
            }
        });
    }

    // --------------------------------------------------------------------------------------------------
    // extension hooks
    // --------------------------------------------------------------------------------------------------

    /**
     * E.g. append basic auth headers ...
     */
    protected void onConnectionCreated(HttpClient client, HttpRequestBase method, Map<String, ? extends Object> params)
            throws IOException
    {

    }

    /**
     * E.g. append oauth params ...
     */
    protected String afterFinalUriConstructed(HttpRequestBase method, String finalUri, Map<String, ? extends Object> params)
    {
        return finalUri;
    }

    // --------------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------------

    private static final Function<String, List<String>> STRING_TO_LIST_STRING = new Function<String, List<String>>()
    {
        @Override
        public List<String> apply(@Nullable String input)
        {
            return Collections.singletonList(input);
        }
    };

    protected void logRequest(HttpRequestBase method, String finalUrl, Map<String, ? extends Object> params)
    {
        final StringBuilder sb = new StringBuilder("{");
        processParams(params, new ParameterProcessor()
        {
            @Override
            public void process(String key, String value)
            {
                if (sb.length() > 1)
                {
                    sb.append(",");
                }
                sb.append(key).append("=").append(value);
            }
        });

        sb.append("}");

        if (log.isDebugEnabled())
        {
            log.debug("[REST call {} {}, Params: {} \nHeaders: {}]", new Object[]{method.getMethod(), finalUrl, sb.toString(), sanitizeHeadersForLogging(method.getAllHeaders())});
        }
    }

    private <T> T requestWithPayload(HttpEntityEnclosingRequestBase method, String uri, Map<String, ? extends Object> params, ResponseCallback<T> callback)
    {
        HttpClient client = httpClientProvider.getHttpClient();
        RemoteResponse response = null;

        HttpResponse httpResponse = null;
        try
        {
            createConnection(client, method, uri, params);
            setPayloadParams(method, params);

            httpResponse = client.execute(method);
            response = checkAndCreateRemoteResponse(method, client, httpResponse);

            return callback.onResponse(response);

        } catch (IOException e)
        {
            log.debug("Failed to execute request: " + method.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + method.getURI(), e);
        } catch (URISyntaxException e)
        {
            log.debug("Failed to execute request: " + method.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + method.getURI(), e);
        } finally
        {
            closeResponse(response);
            SystemUtils.releaseConnection(method, httpResponse);
            if (apiProvider.isCloseIdleConnections())
            {
                httpClientProvider.closeIdleConnections();
            }
        }
    }

    private <T> T requestWithBody(HttpEntityEnclosingRequestBase method, String uri, String body, ContentType contentType,
            ResponseCallback<T> callback)
    {
        HttpClient client = httpClientProvider.getHttpClient();
        RemoteResponse response = null;

        try
        {
            createConnection(client, method, uri, null);
            setBody(method, body, contentType);

            HttpResponse httpResponse = client.execute(method);
            response = checkAndCreateRemoteResponse(method, client, httpResponse);

            return callback.onResponse(response);

        } catch (IOException e)
        {
            log.debug("Failed to execute request: " + method.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + method.getURI(), e);
        } catch (URISyntaxException e)
        {
            log.debug("Failed to execute request: " + method.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + method.getURI(), e);
        } finally
        {
            closeResponse(response);
        }
    }

    private void closeResponse(RemoteResponse response)
    {
        if (response != null)
        {
            response.close();
        }
    }

    private <T> T requestWithoutPayload(HttpRequestBase method, String uri, Map<String, List<String>> parameters, ResponseCallback<T> callback)
    {
        HttpClient client = httpClientProvider.getHttpClient(apiProvider.isCached());

        RemoteResponse response = null;

        HttpResponse httpResponse = null;
        try
        {
            createConnection(client, method, uri + multiParamsToString(parameters, uri.contains("?")), parameters);

            httpResponse = client.execute(method);
            response = checkAndCreateRemoteResponse(method, client, httpResponse);

            return callback.onResponse(response);

        } catch (IOException e)
        {
            log.debug("Failed to execute request: " + method.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + method.getURI(), e);

        } catch (URISyntaxException e)
        {
            log.debug("Failed to execute request: " + method.getURI(), e);
            throw new BitbucketRequestException("Failed to execute request " + method.getURI(), e);
        } finally
        {
            closeResponse(response);
            SystemUtils.releaseConnection(method, httpResponse);
        }
    }

    private RemoteResponse checkAndCreateRemoteResponse(HttpRequestBase method, HttpClient client, HttpResponse httpResponse) throws IOException
    {

        RemoteResponse response = new RemoteResponse();

        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (statusCode >= 300)
        {
            String content = logRequestAndResponse(method, httpResponse, statusCode);

            RuntimeException toBeThrown = new BitbucketRequestException.Other("Error response code during the request : "
                    + statusCode);

            switch (statusCode)
            {
                case HttpStatus.SC_BAD_REQUEST:
                    toBeThrown = new BitbucketRequestException.BadRequest_400();
                    break;
                case HttpStatus.SC_UNAUTHORIZED:
                    toBeThrown = new BitbucketRequestException.Unauthorized_401();
                    break;
                case HttpStatus.SC_FORBIDDEN:
                    toBeThrown = new BitbucketRequestException.Forbidden_403();
                    break;
                case HttpStatus.SC_NOT_FOUND:
                    toBeThrown = new BitbucketRequestException.NotFound_404(method.getMethod() + " " + method.getURI()+" content "+content);
                    break;
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    toBeThrown = new BitbucketRequestException.InternalServerError_500(content);
            }


            throw toBeThrown;
        }

        response.setHttpStatusCode(statusCode);
        if (httpResponse.getEntity() != null)
        {
            response.setResponse(httpResponse.getEntity().getContent());
        }

        return response;
    }

    private String logRequestAndResponse(HttpRequestBase method, HttpResponse httpResponse, int statusCode) throws IOException
    {
        String responseAsString = null;
        if (httpResponse.getEntity() != null)
        {
            InputStream is = httpResponse.getEntity().getContent();
            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer, "UTF-8");
            responseAsString = writer.toString();
        }

        if (log.isWarnEnabled())
        {
            log.warn("Failed to properly execute request [{} {}], \nParams: {}, \nResponse code {}",
                    new Object[]{method.getMethod(), method.getURI(), method.getParams(), statusCode});
        }

        if (log.isDebugEnabled())
        {
            log.debug("Failed to properly execute request [{} {}], \nHeaders: {}, \nParams: {}, \nResponse code {}, response: {}",
                    new Object[]{method.getMethod(), method.getURI(), sanitizeHeadersForLogging(method.getAllHeaders()), method.getParams(),
                            statusCode, responseAsString});
        }

        return responseAsString;
    }

    private Header[] sanitizeHeadersForLogging(Header[] headers)
    {
        List<Header> result = new LinkedList<Header>(Arrays.asList(headers));
        Iterator<Header> iterator = result.iterator();
        while (iterator.hasNext())
        {
            if (iterator.next().getName().toLowerCase().contains("authorization"))
            {
                iterator.remove();
            }
        }
        return result.toArray(new Header[result.size()]);
    }

    protected String paramsToString(Map<String, String> parameters, boolean urlAlreadyHasParams)
    {
        return multiParamsToString(parametersToListParams(parameters), urlAlreadyHasParams);
    }

    protected String multiParamsToString(Map<String, List<String>> parameters, boolean urlAlreadyHasParams)
    {
        StringBuilder queryStringBuilder = new StringBuilder();

        if (parameters != null && !parameters.isEmpty())
        {
            if (!urlAlreadyHasParams)
            {
                queryStringBuilder.append("?");
            } else
            {
                queryStringBuilder.append("&");
            }

            paramsMapToString(parameters, queryStringBuilder);
        }
        return queryStringBuilder.toString();
    }

    private void paramsMapToString(Map<String, List<String>> parameters, StringBuilder builder)
    {
        final Predicate<String> notNullOrEmpty = new Predicate<String>()
        {
            @Override
            public boolean apply(@Nullable String input)
            {
                return input != null && !input.isEmpty();
            }
        };

        builder.append(Joiner.on("&").join(Iterables.concat(Iterables.transform(parameters.entrySet(), new Function<Entry<String, List<String>>, Iterable<String>>()
        {
            @Override
            public Iterable<String> apply(@Nullable final Entry<String, List<String>> entry)
            {
                return Iterables.transform(Iterables.filter(entry.getValue(), notNullOrEmpty), new Function<String, String>()
                {
                    @Override
                    public String apply(@Nullable String entryValue)
                    {
                        return encode(entry.getKey()) + "=" + encode(entryValue);
                    }
                });
            }
        }))));
    }

    private static String encode(String str)
    {
        if (str == null)
        {
            return null;
        }

        try
        {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            throw new BitbucketRequestException("Required encoding not found", e);
        }
    }

    private void createConnection(HttpClient client, HttpRequestBase method, String uri, Map<String, ? extends Object> params)
            throws IOException, URISyntaxException
    {
        String remoteUrl;
        if (uri.startsWith("http:/") || uri.startsWith("https:/"))
        {
            remoteUrl = uri;

        } else
        {
            String apiUrl = uri.startsWith("/api/") ? apiProvider.getHostUrl() : apiProvider.getApiUrl();
            remoteUrl = apiUrl + uri;
        }

        String finalUrl = afterFinalUriConstructed(method, remoteUrl, params);
        method.setURI(new URI(finalUrl));
        //
        logRequest(method, finalUrl, params);
        //
        // something to extend
        //
        onConnectionCreated(client, method, params);

    }

    protected interface ParameterProcessor
    {
        void process(String key, String value);
    }

    private void setPayloadParams(HttpEntityEnclosingRequestBase method, Map<String, ? extends Object> params) throws IOException
    {
        if (params != null)
        {
            final List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            processParams(params, new ParameterProcessor()
            {
                @Override
                public void process(String key, String value)
                {
                    formparams.add(new BasicNameValuePair(key, value));
                }

            });

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            method.setEntity(entity);
        }
    }

    private void setBody(HttpEntityEnclosingRequestBase method, String body, ContentType contentType)
    {
        if (body != null)
        {
            StringEntity entity = new StringEntity(body, contentType);
            method.setEntity(entity);
        }
    }

    protected void processParams(Map<String, ? extends Object> params, ParameterProcessor processParameter)
    {
        if (params == null)
        {
            return;
        }

        for (Entry<String, ? extends Object> entry : params.entrySet())
        {
            Object value = entry.getValue();
            if (value instanceof Collection)
            {
                for (Object v : (Collection<?>) value)
                {
                    if (v != null)
                    {
                        processParameter.process(entry.getKey(), v.toString());
                    }
                }
            } else
            {
                if (value != null)
                {
                    processParameter.process(entry.getKey(), value.toString());
                }
            }
        }
    }
}
