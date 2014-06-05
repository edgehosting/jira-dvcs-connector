package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.testng.annotations.Test;

import java.util.List;

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
    public void willSerializeDefaultGroupsWithSimpleNames()
    {
        String serializedGroups = oDao.serializeDefaultGroups(Sets.newHashSet(new Group("a"), new Group("b")));
        List<String> expectedResults = Lists.newArrayList("a", "b");
        List<String> actualResults = Lists.newArrayList(Splitter.on(";").split(serializedGroups));
        assertThat(expectedResults).containsAll(actualResults);
        assertThat(expectedResults).hasSameSizeAs(actualResults);
    }

    @Test
    public void willSerializeDefaultGroupsWithComplexNames()
    {
        String serializedGroups = oDao.serializeDefaultGroups(Sets.newHashSet(new Group("abraka dab"), new Group("raka")));
        List<String> expectedResults = Lists.newArrayList("raka", "abraka dab");
        List<String> actualResults = Lists.newArrayList(Splitter.on(";").split(serializedGroups));
        assertThat(expectedResults).containsAll(actualResults);
        assertThat(expectedResults).hasSameSizeAs(actualResults);
    }
}
