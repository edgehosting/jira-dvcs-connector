package com.atlassian.jira.plugins.bitbucket.api.impl;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.AuthenticationFactory;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;

public class DefaultAuthenticationFactory implements AuthenticationFactory
{

	public Authentication getAuthentication(SourceControlRepository repository)
	{
		if (StringUtils.isBlank(repository.getUsername()) || StringUtils.isBlank(repository.getPassword()))
		{
			return Authentication.ANONYMOUS;
		}
		
		return Authentication.basic(repository.getUsername(), repository.getPassword());
	}
}
