package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public enum HttpMethod implements ConnectionCreator
{
	GET,
	POST,
	DELETE,
	PUT;

	@Override
	public HttpURLConnection createConnection(String forUri) throws IOException 
    {
		URL url = new URL(forUri);
	    System.setProperty("http.keepAlive", "false");
	    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	    connection.setRequestMethod(name());
		return connection;
	}
}
