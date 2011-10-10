package com.atlassian.jira.plugins.bitbucket.api;

import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.DefaultBitbucketUser;

/**
 * Describes a Bitbucket user
 */
public interface SourceControlUser
{
    static final SourceControlUser UNKNOWN_USER = new DefaultBitbucketUser(
            "unknown", "", "", "https://secure.gravatar.com/avatar/unknown?d=mm", ""
    );

    String getUsername();

    String getFirstName();

    String getLastName();

    String getAvatar();

    String getResourceUri();

}
