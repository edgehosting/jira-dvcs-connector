package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.junit.Test;

import java.io.Serializable;

import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;
import static org.fest.assertions.api.Assertions.assertThat;

public class UserKeyTest
{
    @Test
    public void instancesShouldBeSerializableForUseAsCacheKeys()
    {
        // Set up
        final Serializable original = new CachingCommunicator.UserKey(new Repository(), "foo");

        // Invoke
        final Serializable roundTripped = (Serializable) deserialize(serialize(original));

        // Check
        assertThat(original).isEqualTo(roundTripped);
        assertThat(roundTripped).isEqualTo(original);
        assertThat(original).isNotSameAs(roundTripped);
    }
}
