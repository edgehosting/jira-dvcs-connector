package com.atlassian.jira.plugins.bitbucket.api.impl;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.AuthenticationFactory;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;

public class DefaultAuthenticationFactory implements AuthenticationFactory
{
	@Override
    public Authentication getAuthentication(SourceControlRepository repository)
	{
	    // oAuth
	    if (StringUtils.isNotBlank(repository.getAccessToken()))
	    {
	        return new GithubOAuthAuthentication(repository.getAccessToken());
	    }

	    // basic
	    if (StringUtils.isNotBlank(repository.getAdminUsername()))
	    {
	        return new BasicAuthentication(repository.getAdminUsername(), repository.getAdminPassword());
	    }
	        
	    // none
	    return Authentication.ANONYMOUS;
	}
}
