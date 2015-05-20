package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request;

public abstract class AbstractAuthProvider implements AuthProvider
{
    private final String hostUrl;
    private int apiVersion = 1;
    private boolean cached;
    private boolean closeIdleConnections;

    protected HttpClientProvider httpClientProvider;

    public AbstractAuthProvider(String hostUrl, HttpClientProvider httpClientProvider)
	{
		this.hostUrl = hostUrl;
        this.httpClientProvider = httpClientProvider;
	}

    @Override
	public String getApiUrl()
	{
		return hostUrl.replaceAll("/$", "") + "/api/"  + apiVersion + ".0";
	}

    @Override
    public void setApiVersion(int apiVersion)
    {
        this.apiVersion = apiVersion;
    }

    @Override
    public String getHostUrl()
    {
        return hostUrl;
    }

    @Override
    public boolean isCached()
    {
        return cached;
    }

    @Override
    public void setCached(boolean cached)
    {
        this.cached = cached;
    }

    @Override
    public boolean isCloseIdleConnections()
    {
        return closeIdleConnections;
    }

    @Override
    public void setCloseIdleConnections(final boolean closeIdleConnections)
    {
        this.closeIdleConnections = closeIdleConnections;
    }
}

