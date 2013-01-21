package com.atlassian.jira.plugins.dvcs.service;

public interface InvalidOrganizationManager
{

	void setOrganizationValid(int organizationId, boolean valid);

	boolean isOrganizationValid(int organizationId);
}