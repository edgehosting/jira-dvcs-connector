package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.util.Map;

public interface RemoteRequestor
{

    <T> T get(String uri, Map<String, String> parameters, ResponseCallback<T> callback);
	
    <T> T post(String uri,  Map<String, String> parameters, ResponseCallback<T> callback);
	
    <T> T put(String uri, Map<String, String> parameters, ResponseCallback<T> callback);
	
    <T> T delete(String uri, ResponseCallback<T> callback);//TODO add parameters back
	
}

