package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.util.List;
import java.util.Map;

import org.apache.http.entity.ContentType;

public interface RemoteRequestor
{

    /**
     * Executes get request with the provided parameters.
     * After successful request, {@link ResponseCallback#onResponse(RemoteResponse)} is called on the provided callback.
     *
     * @param uri correctly encoded uri required
     */
    <T> T get(String uri, Map<String, String> parameters, ResponseCallback<T> callback);

    /**
     * Executes get request with the provided parameters.
     * After successful request, {@link ResponseCallback#onResponse(RemoteResponse)} is called on the provided callback.
     *
     * @param uri correctly encoded uri required
     */
    <T> T getWithMultipleVals(String uri, Map<String, List<String>> parameters, ResponseCallback<T> callback);


    /**
     * Executes post request with the provided parameters.
     * If the parameter value is {@link java.util.Collection}, then its values are used as multiple parameters with the key as the name.
     * Otherwise {@link Object#toString()} is used as the value.
     * After successful request, {@link ResponseCallback#onResponse(RemoteResponse)} is called on the provided callback.
     *
     * @param uri correctly encoded uri required
     * @param parameters map of parameters
     * @param callback
     */
    <T> T post(String uri,  Map<String, ? extends Object> parameters, ResponseCallback<T> callback);

    /**
     * Executes post request with the provided body and appropriate content type.
     * 
     * @param uri
     *            correctly encoded uri required
     * @param body
     *            e.g.: json body
     * @param contentType
     *            content type of body
     * @param callback
     * @return response of callback
     */
    <T> T post(String uri, String body, ContentType contentType, ResponseCallback<T> callback);

    /**
     * Executes put request with the provided parameters.
     * After successful request, {@link ResponseCallback#onResponse(RemoteResponse)} is called on the provided callback.
     *
     * @param uri correctly encoded uri required
     */
    <T> T put(String uri, Map<String, String> parameters, ResponseCallback<T> callback);

    /**
     * Executes put request with the provided parameters.
     * After successful request, {@link ResponseCallback#onResponse(RemoteResponse)} is called on the provided callback.
     *
     * @param uri correctly encoded uri required
     * @param body  e.g.: json body
     * @param contentType content type of body
     * @param callback
     *
     */
    <T> T put(String uri, String body, ContentType contentType, ResponseCallback<T> callback);

    /**
     * Executes delete request with the provided parameters.
     * After successful request, {@link ResponseCallback#onResponse(RemoteResponse)} is called on the provided callback.
     *
     * @param uri correctly encoded uri required
     */
    <T> T delete(String uri, Map<String, String> parameters, ResponseCallback<T> callback);
}

