package com.atlassian.jira.plugins.bitbucket.velocity;

import com.atlassian.jira.plugins.bitbucket.spi.CustomStringUtils;
import com.atlassian.theplugin.commons.util.DateUtil;

import java.util.Date;

/**
 *
 */
public class VelocityUtils
{
    public String encodeUriPath(String str)
    {
        return CustomStringUtils.encodeUriPath(str);
    }

    public String getRelativePastDate(Date dateInPast)
    {
        return DateUtil.getRelativePastDate(new Date(), dateInPast);
    }
}
