package com.atlassian.jira.plugins.bitbucket.api.util;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;

import java.util.Date;

/**
 *
 */
public class VelocityUtils
{
    private final DateTimeFormatter dateTimeFormatter;

    public VelocityUtils()
    {
        dateTimeFormatter = ComponentManager.getComponent(DateTimeFormatterFactory.class).formatter();
    }

    public String encodeUriPath(String str)
    {
        return CustomStringUtils.encodeUriPath(str);
    }

    public String getRelativePastDate(Date dateInPast)
    {
        return dateTimeFormatter.forLoggedInUser().withStyle(DateTimeStyle.RELATIVE).format(dateInPast);
    }
}
