package com.atlassian.jira.plugins.dvcs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.web.action.JiraWebActionSupport;

public class SystemUtils
{
	private static final Logger log = LoggerFactory.getLogger(SystemUtils.class);

	private static final boolean SAFE_REDIRECT_EXISTS;
	
	static
	{
	    SAFE_REDIRECT_EXISTS = getRedirectExists();
	}
	
	private static boolean getRedirectExists()
	{
	    // Detect whether getRedirect(String, boolean) is present
	    try {
            JiraWebActionSupport.class.getMethod("getRedirect", String.class, Boolean.class);
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
}
