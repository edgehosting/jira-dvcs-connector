package com.atlassian.jira.plugins.dvcs.model;

import org.testng.annotations.Test;

import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;
import static org.fest.assertions.api.Assertions.assertThat;

public class SmartCommitErrorTest
{
    @Test
    public void instancesShouldBeSerializableForUseWithinCacheKeys()
    {
        // Set up
        final String error = "theError";
        final String node = "theNode";
        final String url = "theUrl";
        final SmartCommitError original = new SmartCommitError(node, url, error);

        // Invoke
        final SmartCommitError roundTripped = (SmartCommitError) deserialize(serialize(original));

        // Check
        assertThat(roundTripped.getCommitUrl()).isEqualTo(url);
        assertThat(roundTripped.getError()).isEqualTo(error);
        assertThat(roundTripped.getShortChangesetNode()).isEqualTo(node);
        assertThat(original).isNotSameAs(roundTripped);
    }
}
