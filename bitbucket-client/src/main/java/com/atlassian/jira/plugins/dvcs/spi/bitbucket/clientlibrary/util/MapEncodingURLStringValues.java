package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * MapEncodingURLStringValues
 * 
 * @author Martin Skurla mskurla@atlassian.com
 */
public final class MapEncodingURLStringValues extends LinkedHashMap<String, String>
{
    private final String encoding;
            
    
    private MapEncodingURLStringValues(Map<String, String> map, String encoding)
    {
        this.encoding = encoding;
        
        for (Entry<String, String> entry : map.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }
    
    
    public static MapEncodingURLStringValues fromMap(Map<String, String> map, String encoding)
    {
        return new MapEncodingURLStringValues(map, encoding);
    }
    

    @Override
    public String put(String key, String value)
    {
        String encodedValue = encodeStringValue(value);
        
        return super.put(key, encodedValue);
    }
    
    private String encodeStringValue(String inputValue)
    {
		if (inputValue == null)
		{
			return null;
		}

		try
		{
			return URLEncoder.encode(inputValue, encoding);
		} catch (UnsupportedEncodingException e)
		{
			throw new IllegalStateException("Required encoding not found", e);
		}
    }
}
