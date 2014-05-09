package com.atlassian.jira.plugins.dvcs.util;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileAction;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class CustomStringUtils
{

    public static String encode(String str)
    {
        if (str == null)
        {
            return null;
        }
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
    
}
