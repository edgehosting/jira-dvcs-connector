package it.com.atlassian.jira.plugins.bitbucket;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;

import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import com.google.common.collect.Maps;

public class RestUrlBuilder
{
    private static JiraTestedProduct jira;

    private final String path;
    private final Map<String, String> params = Maps.newHashMap();

    public RestUrlBuilder(String path)
    {
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
//        url.append(getBaseUrl());
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
     * @return
     */
    private String getBaseUrlFast()
    {
        try
        {
            String hostname = InetAddress.getLocalHost().getHostName();
            return "http://" + hostname + ":2990/jira";
        } catch (UnknownHostException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * This method should always return correct base url, but is pretty slow (it starts browser)
     * @return
     */
    private String getBaseUrl()
    {
        if (jira == null)
        {
            jira = TestedProductFactory.create(JiraTestedProduct.class);
        }
        return jira.getProductInstance().getBaseUrl();
    }
}