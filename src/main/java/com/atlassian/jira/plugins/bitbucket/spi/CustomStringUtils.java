package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class CustomStringUtils
{

    public static final String encode(String str) {
        try
        {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            throw new SourceControlException("required encoding not found", e);
        }
    }


}
