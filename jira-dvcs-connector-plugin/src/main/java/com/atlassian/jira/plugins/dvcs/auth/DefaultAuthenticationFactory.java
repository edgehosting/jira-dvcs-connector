package com.atlassian.jira.plugins.dvcs.auth;

import com.atlassian.jira.plugins.dvcs.model.Credential;
import org.apache.commons.lang.StringUtils;

public class DefaultAuthenticationFactory implements AuthenticationFactory
{
	@Override
    public Authentication getAuthentication(Credential credential)
	{
	    // oAuth
	    if (StringUtils.isNotBlank(credential.getAccessToken()))
	    {
	        return new GithubOAuthAuthentication(credential.getAccessToken());
	    }

	    // basic
	    if (StringUtils.isNotBlank(credential.getAdminUsername()))
	    {
	        return new BasicAuthentication(credential.getAdminUsername(), credential.getAdminPassword());
	    }
	        
	    // none
	    return Authentication.ANONYMOUS;
	}
}
