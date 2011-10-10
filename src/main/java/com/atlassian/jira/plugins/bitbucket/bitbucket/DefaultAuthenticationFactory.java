package com.atlassian.jira.plugins.bitbucket.bitbucket;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.bitbucket.common.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.mapper.Encryptor;

public class DefaultAuthenticationFactory implements AuthenticationFactory
{
	private final Encryptor encryptor;

	public DefaultAuthenticationFactory(Encryptor encryptor)
	{
		this.encryptor = encryptor;
	}

	public Authentication getAuthentication(SourceControlRepository repository)
	{
		if (StringUtils.isBlank(repository.getUsername()) || StringUtils.isBlank(repository.getPassword()))
		{
			return Authentication.ANONYMOUS;
		}
		
		return Authentication.basic(repository.getUsername(),
				encryptor.decrypt(repository.getPassword(), repository.getProjectKey(),
						repository.getUrl()));
	}
}
