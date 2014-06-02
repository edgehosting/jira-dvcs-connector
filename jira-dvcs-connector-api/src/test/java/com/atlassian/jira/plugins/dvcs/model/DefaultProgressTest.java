package com.atlassian.jira.plugins.dvcs.model;

import org.testng.annotations.Test;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;
import static org.fest.assertions.api.Assertions.assertThat;

public class DefaultProgressTest
{
    @Test
    public void instancesShouldBeSerializableForUseWithinCacheKeys()
    {
        // Set up
        final DefaultProgress original = new DefaultProgress();
        original.setSmartCommitErrors(singletonList(new SmartCommitError("abcdefg", "b", "c")));

        // Invoke
        final DefaultProgress roundTripped = (DefaultProgress) deserialize(serialize(original));

        // Check
        assertThat(roundTripped.getSmartCommitErrors().size()).isEqualTo(1);
        final SmartCommitError smartCommitError = roundTripped.getSmartCommitErrors().get(0);
        assertThat(smartCommitError.getCommitUrl()).isEqualTo("b");
    }
}
