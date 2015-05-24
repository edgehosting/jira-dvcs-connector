package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import java.io.Serializable;

/**
 *
 * BitbucketRepositoryInfo
 * <pre>
 * {
 *   "links": [...],
 *   "full_name": "bitbucket/bitbucket"
 * },
 * </pre>
 *
 * @author Miroslav Stencel mstencel@atlassian.com
 *
 */

public class BitbucketRepositoryInfo implements Serializable
{
    private static final long serialVersionUID = -5735304704087407759L;

    private String fullName;

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName(String fullName)
    {
        this.fullName = fullName;
    }
}
