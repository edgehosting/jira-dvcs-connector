package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;

/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public class SystemUtils
{
    private SystemUtils() {}


    public static final String encodeUsingBase64(String input)
    {
        try
        {
            byte[] encodedBytes = Base64.encodeBase64(input.getBytes("UTF-8"));

            return new String(encodedBytes, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new AssertionError();
        }
    }
}
