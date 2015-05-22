package com.atlassian.jira.plugins.dvcs.auth.impl;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class DefaultAuthenticationFactory implements AuthenticationFactory
{

	private final Encryptor encryptor;

    @Autowired
	public DefaultAuthenticationFactory(Encryptor encryptor)
	{
		this.encryptor = checkNotNull(encryptor);
	}

	@Override
	public Authentication getAuthentication(Repository repository)
	{
		Credential credential = repository.getCredential();

		// oAuth
		if (StringUtils.isNotBlank(credential.getAccessToken()))
		{
			return new OAuthAuthentication(credential.getAccessToken());
		}

		// none
		return Authentication.ANONYMOUS;
	}

	@Override
	public Authentication getAuthentication(Organization organization)
	{
		Credential credential = organization.getCredential();
		// oAuth
		if (StringUtils.isNotBlank(credential.getAccessToken()))
		{
			return new OAuthAuthentication(credential.getAccessToken());
		}

		// none
		return Authentication.ANONYMOUS;
	}

	private String decryptPassword(Credential credential, String orgName, String orgHostUrl)
	{
		return encryptor.decrypt(credential.getAdminPassword(), orgName, orgHostUrl);
	}

}
