package com.atlassian.jira.plugins.dvcs.model;

import org.testng.annotations.Test;

import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;
import static org.fest.assertions.api.Assertions.assertThat;

public class RepositoryTest
{
    @Test
    public void instancesShouldBeSerializableForUseWithinCacheKeys()
    {
        // Set up
        final Repository original = new Repository();
        original.setCredential(new Credential("abcdefg", "x", "y"));

        // Invoke
        final Repository roundTripped = (Repository) deserialize(serialize(original));

        // Check
        assertThat(original).isEqualTo(roundTripped);
        assertThat(roundTripped).isEqualTo(original);
        assertThat(original).isNotSameAs(roundTripped);
    }
}
