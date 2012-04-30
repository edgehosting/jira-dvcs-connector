package com.atlassian.jira.plugins.bitbucket.api.util;

import com.atlassian.jira.plugins.bitbucket.api.ChangesetFileAction;
import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class CustomStringUtils
{

    // TODO: check all calls this method if correctly calls method encode() not encodeUriPath() !!!
    public static String encode(String str)
    {
        try
        {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            throw new SourceControlException("required encoding not found", e);
        }
    }

    public static ChangesetFileAction getChangesetFileAction(String changeAction)
    {
        if ("added".equals(changeAction))
        {
            return ChangesetFileAction.ADDED;
        } else if ("removed".equals(changeAction))
        {
            return ChangesetFileAction.REMOVED;
        } else
        {
            return ChangesetFileAction.MODIFIED;
        }
    }


    public static String encodeUriPath(String str)
    {
        try
        {
            return URIUtil.encodePath(str);
        } catch (URIException e)
        {
            e.printStackTrace();
            return "invalid-path";
        }
    }
}
