package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;


import org.junit.Test;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketAccount;

import static org.fest.assertions.api.Assertions.*;


/**
 * @author Martin Skurla
 */
public class ClientUtilsTest
{

    @Test
    public void jsonMembersWithUnderscores_ShouldBeCorrectlyTransformedIntoJavaFields() // Java Naming Conventions
    {
        BitbucketAccount bitbucketAccount = ClientUtils.fromJson(
            "{\"username\": \"baratrion\"," +
            " \"first_name\": \"Mehmet S\"," +
            " \"last_name\": \"Catalbas\"," +
            " \"avatar\": \"https://secure.gravatar.com/avatar/55a1369161d3a648729b59cabf160e70?d=identicon&s=32\"," +
            " \"resource_uri\": \"/1.0/users/baratrion\"}", BitbucketAccount.class);
        
        assertThat(bitbucketAccount.getFirstName())  .isEqualTo("Mehmet S");
        assertThat(bitbucketAccount.getLastName())   .isEqualTo("Catalbas");
        assertThat(bitbucketAccount.getResourceUri()).isEqualTo("/1.0/users/baratrion");
    }
}
