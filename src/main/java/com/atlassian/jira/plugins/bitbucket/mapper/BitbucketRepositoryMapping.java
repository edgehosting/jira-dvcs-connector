package com.atlassian.jira.plugins.bitbucket.mapper;

import com.atlassian.jira.plugins.bitbucket.bitbucket.Authentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketRepository;
import org.apache.commons.lang.StringUtils;

/**
 * The mapping information between a {@link BitbucketRepository} and a jira project
 */
public class BitbucketRepositoryMapping
{
    private final String projectKey;
    private final BitbucketRepository bitbucketRepository;
    private final String username;
    private final String password;
    private final String branch;

    public BitbucketRepositoryMapping(String projectKey, BitbucketRepository bitbucketRepository,
                                      String branch, String username, String password)
    {
        this.projectKey = projectKey;
        this.bitbucketRepository = bitbucketRepository;
        this.username = username;
        this.password = password;
        this.branch = branch;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public BitbucketRepository getBitbucketRepository()
    {
        return bitbucketRepository;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public String getBranch()
    {
        return branch;
    }

    public Authentication getBitbucketAuthentication()
    {
        Authentication auth = Authentication.ANONYMOUS;
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
            auth = Authentication.basic(username, password);
        return auth;
    }
}
