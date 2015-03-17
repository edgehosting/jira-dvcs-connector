package com.atlassian.jira.plugins.dvcs.model;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

    @Test
    public void testClonedInstance()
    {
        final Organization original = new Organization();
        final List<Group> groups = ImmutableList.of(new Group("slug", "group"));
        final Set<Group> defaultGroups = Sets.newHashSet();
        defaultGroups.add(new Group("slug", "default"));

        original.setAutolinkNewRepos(true);
        original.setCredential(new Credential("oauthKey", "oauthSecret", "accessToken"));
        original.setDefaultGroups(defaultGroups);
        original.setDvcsType("bitbucket");
        original.setHostUrl("hostUrl");
        original.setId(100);
        original.setName("name");
        original.setGroups(groups);
        original.setOrganizationUrl("url");
        original.setSmartcommitsOnNewRepos(true);
        original.setRepositories(ImmutableList.of(mock(Repository.class)));

        Organization clonedOrg = original.clone();

        assertThat(clonedOrg).isNotSameAs(original);

        EqualsBuilder.reflectionEquals(clonedOrg, original);
    }
}