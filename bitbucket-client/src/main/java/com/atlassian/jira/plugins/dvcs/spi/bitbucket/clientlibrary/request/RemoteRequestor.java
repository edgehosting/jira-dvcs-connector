package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.util.Map;

public interface RemoteRequestor
{

    /**
     * @param uri correctly encoded uri required
     */
    <T> T get(String uri, Map<String, String> parameters, ResponseCallback<T> callback);

    /**
     * @param uri correctly encoded uri required
     */
    <T> T post(String uri,  Map<String, ? extends Object> parameters, ResponseCallback<T> callback);

    /**
     * @param uri correctly encoded uri required
     */
    <T> T put(String uri, Map<String, String> parameters, ResponseCallback<T> callback);

    /**
     * @param uri correctly encoded uri required
     */
    <T> T delete(String uri, Map<String, String> parameters, ResponseCallback<T> callback);
}

