package com.atlassian.jira.plugins.dvcs.util;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.web.action.JiraWebActionSupport;

public class SystemUtils
{
    private static final Logger log = LoggerFactory.getLogger(SystemUtils.class);

    private static final boolean SAFE_REDIRECT_EXISTS;
    private static final boolean URL_VALIDATOR_EXISTS;
    
    static
    {
        SAFE_REDIRECT_EXISTS = getRedirectExists();
        URL_VALIDATOR_EXISTS = getUrlValidatorExists();
    }
    
    private static boolean getRedirectExists()
    {
        // Detect whether getRedirect(String, boolean) is present
        return getMethodExists(JiraWebActionSupport.class, "getRedirect", String.class, boolean.class);
    }
    
    private static boolean getUrlValidatorExists()
    {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("com.atlassian.jira.util.UrlValidator");
        } catch (ClassNotFoundException e)
        {
        }
        if ( clazz != null )
        {
            return getMethodExists(clazz, "isValid", String.class);
        }

        return false;
    }
    
    private static boolean getMethodExists(Class<?> clazz, String method, Class<?>... parameterTypes)
    {
        try
        {
            clazz.getMethod(method, parameterTypes);
        } catch (NoSuchMethodException nsme)
        {
            return false;
        }
        
        return true;
    }
    
    public static long getSystemPropertyLong(String propertyName, long defaultValue)
    {
        String property = System.getProperty(propertyName, "" + defaultValue);
        try
        {
            return Long.parseLong(property);
        } catch (Exception e)
        {
            log.warn("Unable to parse system property [" + propertyName + "] with value ["
                    + property + "]. Returning default value [" + defaultValue + "]. "
                    + e.getMessage());
            return defaultValue;
        }
    }
    
    public static String getRedirect(JiraWebActionSupport action, String url, boolean unsafe)
    {
        if ( SAFE_REDIRECT_EXISTS )
        {
            return action.getRedirect(url, unsafe);
        } else
        {
            return action.getRedirect(url);
        }
    }
       
    public static boolean isValid(String url)
    {
        if ( URL_VALIDATOR_EXISTS )
        {
            return com.atlassian.jira.util.UrlValidator.isValid(url);
        } else
        {
        	try
        	{
        		new URL(url);
        	} catch (MalformedURLException e)
        	{
        		return false;
        	}
           return true;
        }
    }
}
