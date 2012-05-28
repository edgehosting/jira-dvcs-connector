package com.atlassian.jira.plugins.dvcs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemUtils
{
	private static final Logger log = LoggerFactory.getLogger(SystemUtils.class);

	public static long getSystemPropertyLong(String propertyName, long defaultValue)
	{
		String property = System.getProperty(propertyName, "" + defaultValue);
		try
		{
			return Long.getLong(property);
		} catch (Exception e)
		{
			log.warn("Unable to parse system property [" + propertyName + "] with value ["
			        + property + "]. Returning default value [" + defaultValue + "]. "
			        + e.getMessage());
			return defaultValue;
		}
	}

}
