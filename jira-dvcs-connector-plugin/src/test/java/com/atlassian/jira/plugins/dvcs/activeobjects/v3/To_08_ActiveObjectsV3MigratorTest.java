package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class To_08_ActiveObjectsV3MigratorTest
{

    @Test
    public void testExtractProjectKey()
    {
        To_08_ActiveObjectsV3Migrator migrator = new To_08_ActiveObjectsV3Migrator(null);
        
        assertThat(migrator.extractProjectKey("ABC-1"))      .isEqualTo("ABC");
        assertThat(migrator.extractProjectKey("   ABC-1   ")).isEqualTo("ABC");
        
        assertThat(migrator.extractProjectKey(" abc-1")).isEqualTo("abc");

        assertThat(migrator.extractProjectKey(null))         .isNull();
        assertThat(migrator.extractProjectKey(""))           .isNull();
        assertThat(migrator.extractProjectKey(" -1"))        .isNull();
        assertThat(migrator.extractProjectKey(" abc"))       .isNull();
        assertThat(migrator.extractProjectKey(" abc - 123 ")).isNull();
    }

}
