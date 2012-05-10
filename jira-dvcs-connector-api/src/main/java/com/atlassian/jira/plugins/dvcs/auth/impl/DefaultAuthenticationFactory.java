package com.atlassian.jira.plugins.dvcs.auth.impl;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Repository;

public class DefaultAuthenticationFactory implements AuthenticationFactory
{

	private final Encryptor encryptor;

	public DefaultAuthenticationFactory(Encryptor encryptor)
	{
		this.encryptor = encryptor;
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

	    // basic
	    if (StringUtils.isNotBlank(credential.getAdminUsername()))
	    {
	        return new BasicAuthentication(credential.getAdminUsername(), decryptPassword(credential, repository));
	    }
	        
	    // none
	    return Authentication.ANONYMOUS;
	}

	private String decryptPassword(Credential credential, Repository repository)
	{
		return encryptor.decrypt(credential.getAdminPassword(), repository.getOrgName(), repository.getOrgHostUrl());
	}
}
