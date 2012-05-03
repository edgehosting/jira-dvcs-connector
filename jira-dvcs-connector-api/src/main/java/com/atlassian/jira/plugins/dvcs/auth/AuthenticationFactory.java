package com.atlassian.jira.plugins.dvcs.auth;

import com.atlassian.jira.plugins.dvcs.model.Credential;

public interface AuthenticationFactory
{
	public Authentication getAuthentication(Credential credential);
}
