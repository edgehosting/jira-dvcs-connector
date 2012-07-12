package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.util.Map;

public interface RemoteRequestor
{

	RemoteResponse get(String uri, Map<String, String> parameters);
	
	RemoteResponse post(String uri, Map<String, String> parameters, Object payload); 
	
	RemoteResponse put(String uri, Map<String, String> parameters, Object payload);
	
	RemoteResponse delete(String uri); 
	
}

