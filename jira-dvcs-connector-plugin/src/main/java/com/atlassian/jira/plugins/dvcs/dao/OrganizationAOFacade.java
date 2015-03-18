package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.model.Organization;

import java.util.Collection;
import java.util.List;

public interface OrganizationAOFacade
{
    List<Organization> getAll();

    Organization save(Organization organization);

    void remove(int organizationId);

    void setDefaultGroupsSlugs(int orgId, Collection<String> groupsSlugs);
}
