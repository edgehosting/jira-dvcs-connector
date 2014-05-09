package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client;


import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import org.testng.annotations.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.fest.assertions.api.Assertions.assertThat;


/**
 * @author Martin Skurla
 */
public class ClientUtilsTest
{
    private static final DateFormat UTC_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z") {
        {
            setTimeZone(TimeZone.getTimeZone("UTC"));
        }
    };


    @Test
    public void jsonMembersWithUnderscores_ShouldBeCorrectlyTransformedIntoJavaFields() // Java Naming Conventions
    {
        BitbucketAccount bitbucketAccount = ClientUtils.fromJson(
            "{" +
            "  \"username\"    : \"baratrion\"," +
            "  \"first_name\"  : \"Mehmet S\"," +
            "  \"last_name\"   : \"Catalbas\"," +
            "  \"avatar\"      : \"https://secure.gravatar.com/avatar/55a1369161d3a648729b59cabf160e70?d=identicon&s=32\"," +
            "  \"resource_uri\": \"/1.0/users/baratrion\"" +
            "}", BitbucketAccount.class);
        
        assertThat(bitbucketAccount.getFirstName())  .isEqualTo("Mehmet S");
        assertThat(bitbucketAccount.getLastName())   .isEqualTo("Catalbas");
        assertThat(bitbucketAccount.getResourceUri()).isEqualTo("/1.0/users/baratrion");
    }

    @Test
    public void jsonTimestamps_ShouldBeParsedAgainstUTCTimeZone()
    {
        BitbucketChangeset bitbucketChangeset = ClientUtils.fromJson(
                "{" +
                "  \"raw_author\"  : \"Mary Anthony <manthony@172-28-13-105.staff.sf.atlassian.com>\"," +
                "  \"utctimestamp\": \"2012-07-23 22:26:36+00:00\"," +
                "  \"author\"      : \"Mary Anthony\"" +
                "}", BitbucketChangeset.class);

        assertThat(UTC_DATE_FORMAT.format(bitbucketChangeset.getUtctimestamp())).isEqualTo("2012-07-23 22:26:36 UTC");
    }
}
