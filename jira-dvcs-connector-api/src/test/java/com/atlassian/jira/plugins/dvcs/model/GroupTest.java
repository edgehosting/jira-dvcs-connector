package com.atlassian.jira.plugins.dvcs.model;

import org.testng.annotations.Test;

import java.io.Serializable;

import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;
import static org.fest.assertions.api.Assertions.assertThat;

public class GroupTest
{
    @Test
    public void instancesShouldBeSerializableForUseWithinCacheKeys()
    {
        // Set up
        final Serializable original = new Group("theSlug", "theNiceName");

        // Invoke
        final Serializable roundTripped = (Serializable) deserialize(serialize(original));

        // Check
        assertThat(original).isEqualTo(roundTripped);
        assertThat(roundTripped).isEqualTo(original);
        assertThat(original).isNotSameAs(roundTripped);
    }
}
