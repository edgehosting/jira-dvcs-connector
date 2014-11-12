package com.atlassian.jira.plugins.dvcs.activeobjects;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.google.common.collect.ImmutableMap;
import net.java.ao.EntityManager;

public class OrganizationAOPopulator extends AOPopulator
{
    EntityManager entityManager;

    public OrganizationAOPopulator(final EntityManager entityManager)
    {
        super(entityManager);
    }

    public OrganizationMapping create(String dvcsType)
    {
        return create(OrganizationMapping.class, ImmutableMap.<String, Object>of(OrganizationMapping.DVCS_TYPE, dvcsType));
    }
}
