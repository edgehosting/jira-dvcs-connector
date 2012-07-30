package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

import java.io.IOException;
import java.net.HttpURLConnection;

public interface ConnectionCreator {
	
	HttpURLConnection createConnection(String forUri) throws IOException;
	
}