package com.atlassian.jira.plugins.dvcs.pageobjects;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.google.common.collect.Maps;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class to assist with constructing urls when querying rest resources during integration tests.
 * <p/>
 * If the constructor {@code RestUrlBuilder(String)} is used, then an JIRA base url of "http://" + hostname + ":2990/jira"
 * is assumed, and this is returned from {@code getBaseUrlFast()}. If the constructor
 * {@code RestUrlBuilder(JiraTestedProduct, String)} is used, then the {@code JiraTestedProduct} is queried to get the
 * actual base url of the JIRA instance when {@code getBaseUrlFast()} is called.
 */
public class RestUrlBuilder
{
    private final JiraTestedProduct jira;
    private final Map<String, String> params = Maps.newHashMap();
    private final String path;

    public RestUrlBuilder(String path)
    {
        this.jira = null;
        this.path = path;
    }

    public RestUrlBuilder(JiraTestedProduct jira, String path)
    {
        this.jira = jira;
        this.path = path;
    }

    public RestUrlBuilder add(String name, String value)
    {
        params.put(name, value);
        return this;
    }

    public String build()
    {
        StringBuilder url = new StringBuilder();
        url.append(getBaseUrlFast());
        url.append(path);
        url.append(url.indexOf("?") != -1 ? "&" : "?");
        url.append("os_username=admin&os_password=admin");
        for (Entry<String, String> entry : params.entrySet())
        {
            url.append("&");
            url.append(entry.getKey());
            url.append("=");
            url.append(entry.getValue());
        }
        return url.toString();
    }

    @Override
    public String toString()
    {
        return build();
    }

    /**
     * Fast way to get base url. (assuming it's http://hostname:2990/jira)
     *
     * Note: if {@code jira} has been instantiated then we use it to get the real url.
     *
     * @return
     */
    private String getBaseUrlFast()
    {
        if (jira == null)
        {
            try
            {
                String hostname = InetAddress.getLocalHost().getHostName();
                return "http://" + hostname + ":2990/jira";
            } catch (UnknownHostException e)
            {
                throw new RuntimeException(e);
            }
        } else
        {
            return jira.environmentData().getBaseUrl().toExternalForm();
        }
    }
}
