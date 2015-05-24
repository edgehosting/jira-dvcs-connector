package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.model.Organization;

import java.util.Collection;
import java.util.List;

public interface OrganizationAOFacade
{
    List<Organization> fetch();

    Organization save(Organization organization);

    void remove(int organizationId);

    void updateDefaultGroupsSlugs(int orgId, Collection<String> groupsSlugs);
}
