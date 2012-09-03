package com.atlassian.jira.plugins.dvcs.dao.impl;

import static org.junit.Assert.*;

import org.junit.Test;

import com.atlassian.jira.plugins.dvcs.model.Group;
import com.google.common.collect.Sets;

public class OrganizationDaoImplTest
{
    private final OrganizationDaoImpl oDao = new OrganizationDaoImpl(null, null);

    @Test
    public void testDeserializeDefaultGroups()
    {
        assertEquals(Sets.newHashSet(new Group("a"),new Group("b")), oDao.deserializeDefaultGroups("a;b"));
        assertEquals(Sets.newHashSet(), oDao.deserializeDefaultGroups(null));
        assertEquals(Sets.newHashSet(), oDao.deserializeDefaultGroups(" "));
        assertEquals(Sets.newHashSet(), oDao.deserializeDefaultGroups(";"));
        assertEquals(Sets.newHashSet(), oDao.deserializeDefaultGroups(" ;"));
        assertEquals(Sets.newHashSet(new Group("abraka dab"),new Group("raka")), oDao.deserializeDefaultGroups("abraka dab;raka"));
    }
    
    @Test
    public void testSerializeDefaultGroups()
    {
        assertEquals("a;b",oDao.serializeDefaultGroups(Sets.newHashSet(new Group("a"),new Group("b"))));
        assertEquals("raka;abraka dab",oDao.serializeDefaultGroups(Sets.newHashSet(new Group("abraka dab"),new Group("raka"))));
    }

}
