package com.atlassian.jira.plugins.dvcs.model;

import org.testng.annotations.Test;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;
import static org.fest.assertions.api.Assertions.assertThat;

public class OrganizationTest
{
    @Test
    public void instancesShouldBeSerializableForUseWithinCacheKeys()
    {
        // Set up
        final Organization original = new Organization();
        original.setDefaultGroups(singleton(new Group("slug", "name")));
        original.setRepositories(singletonList(new Repository()));

        // Invoke
        final Organization roundTripped = (Organization) deserialize(serialize(original));

        // Check
        assertThat(original).isEqualTo(roundTripped);
        assertThat(roundTripped).isEqualTo(original);
        assertThat(original).isNotSameAs(roundTripped);
    }
}
