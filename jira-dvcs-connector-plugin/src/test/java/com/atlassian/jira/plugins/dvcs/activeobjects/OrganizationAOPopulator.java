package com.atlassian.jira.plugins.dvcs.activeobjects;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.google.common.collect.ImmutableMap;
import net.java.ao.EntityManager;

import java.util.Map;

public class OrganizationAOPopulator extends AOPopulator
{
    EntityManager entityManager;

    public OrganizationAOPopulator(final EntityManager entityManager)
    {
        super(entityManager);
    }

    public OrganizationMapping create(String dvcsType)
    {
        return create(dvcsType, "https://bitbucket.org", "fusionaccount");
    }

    public OrganizationMapping create(String dvcsType, String hostUrl, String name)
    {
        final Map<String, Object> params = ImmutableMap.<String, Object>of(
                OrganizationMapping.DVCS_TYPE, dvcsType,
                OrganizationMapping.HOST_URL, hostUrl,
                OrganizationMapping.NAME, name);

        return create(OrganizationMapping.class, params);
    }
}
