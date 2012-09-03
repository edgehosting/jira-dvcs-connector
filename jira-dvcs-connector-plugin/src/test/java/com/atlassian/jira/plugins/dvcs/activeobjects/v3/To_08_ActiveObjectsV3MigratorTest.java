package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import static org.junit.Assert.*;

import org.junit.Test;

public class To_08_ActiveObjectsV3MigratorTest
{

    @Test
    public void testExtractProjectKey()
    {
        To_08_ActiveObjectsV3Migrator migrator = new To_08_ActiveObjectsV3Migrator(null);
        assertEquals("ABC", migrator.extractProjectKey("ABC-1"));
        assertEquals("ABC", migrator.extractProjectKey("   ABC-1   "));
        assertEquals("abc", migrator.extractProjectKey(" abc-1"));
        assertEquals(null, migrator.extractProjectKey(null));
        assertEquals(null, migrator.extractProjectKey(""));
        assertEquals(null, migrator.extractProjectKey(" -1"));
        assertEquals(null, migrator.extractProjectKey(" abc"));
        assertEquals(null, migrator.extractProjectKey(" abc - 123 "));
    }

}
