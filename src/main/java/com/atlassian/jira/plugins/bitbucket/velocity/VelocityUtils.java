package com.atlassian.jira.plugins.bitbucket.velocity;

import com.atlassian.jira.plugins.bitbucket.spi.CustomStringUtils;

/**
 *
 */
public class VelocityUtils
{
    public String encodeUtf8(String str)
    {
        return CustomStringUtils.encode(str);
    }
}
