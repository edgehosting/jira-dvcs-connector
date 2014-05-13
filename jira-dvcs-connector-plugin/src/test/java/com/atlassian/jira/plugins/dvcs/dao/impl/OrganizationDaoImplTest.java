package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.google.common.collect.Sets;
import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class OrganizationDaoImplTest
{
    private final OrganizationDaoImpl oDao = new OrganizationDaoImpl(null, null, null);

    @Test
    public void testDeserializeDefaultGroups()
    {
        assertThat(oDao.deserializeDefaultGroups(null)).isEmpty();
        assertThat(oDao.deserializeDefaultGroups(" ")) .isEmpty();
        assertThat(oDao.deserializeDefaultGroups(";")) .isEmpty();
        assertThat(oDao.deserializeDefaultGroups(" ;")).isEmpty();

        assertThat(oDao.deserializeDefaultGroups("a;b")).containsOnly(new Group("a"), new Group("b"));

        assertThat(oDao.deserializeDefaultGroups("abraka dab;raka")).containsOnly(new Group("abraka dab"),new Group("raka"));
    }
    
    @Test
    public void testSerializeDefaultGroups()
    {
        assertThat(oDao.serializeDefaultGroups(Sets.newHashSet(new Group("a"),new Group("b"))))
                .isEqualTo("a;b");

        assertThat(oDao.serializeDefaultGroups(Sets.newHashSet(new Group("abraka dab"),new Group("raka"))))
                .isEqualTo("raka;abraka dab");
    }
}
