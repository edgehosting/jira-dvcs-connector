package com.atlassian.jira.plugins.dvcs.service;

public interface InvalidOrganizationManager
{

    void validateOrganization(int organizationId);

    void invalidateOrganization(int organizationId);

    boolean isInvalidOrganization(int organizationId);

}