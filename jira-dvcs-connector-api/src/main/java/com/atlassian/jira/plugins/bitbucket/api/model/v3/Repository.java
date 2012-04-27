package com.atlassian.jira.plugins.bitbucket.api.model.v3;

import net.java.ao.Entity;

public interface Repository extends Entity {
	
	int getOrganizationId();
	String getRepositoryUri();

	void setOrganizationId(int organizationId);
	void setRepositoryUri(String repositoryUrl);

}
