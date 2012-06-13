package com.atlassian.jira.plugins.dvcs.auth;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface AuthenticationFactory
{
	public Authentication getAuthentication(Repository repository);

	public Authentication getAuthentication(Organization organization);
}
